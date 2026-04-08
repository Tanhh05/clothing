package com.clothing.service.impl;

import com.clothing.config.MomoProperties;
import com.clothing.entity.OrderEntity;
import com.clothing.entity.PaymentEntity;
import com.clothing.exception.BusinessException;
import com.clothing.repository.PaymentRepository;
import com.clothing.service.PaymentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class MomoPaymentServiceImpl implements PaymentService {
    private static final Logger log = LoggerFactory.getLogger(MomoPaymentServiceImpl.class);

    private static final String METHOD_MOMO = "MOMO";
    private static final String STATUS_CREATED = "CREATED";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_FAILED = "FAILED";

    private final RestTemplate restTemplate;
    private final MomoProperties momoProperties;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;

    public MomoPaymentServiceImpl(MomoProperties momoProperties, PaymentRepository paymentRepository) {
        this.restTemplate = new RestTemplate();
        this.momoProperties = momoProperties;
        this.paymentRepository = paymentRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String createMomoPayment(OrderEntity order) {
        validateConfig();

        String orderId = "ORD" + order.getId() + "-" + System.currentTimeMillis();
        String requestId = "REQ" + order.getId() + "-" + System.currentTimeMillis();
        String amount = String.valueOf(order.getTotalPrice());
        String orderInfo = "Thanh toan don hang #" + order.getId();
        String extraData = Base64.getEncoder().encodeToString(("orderId=" + order.getId()).getBytes(StandardCharsets.UTF_8));
        String requestType = momoProperties.getRequestType();
        String partnerClientId = "";

        String rawSignature = "accessKey=" + momoProperties.getAccessKey()
                + "&amount=" + amount
                + "&extraData=" + extraData
                + "&ipnUrl=" + momoProperties.getIpnUrl()
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + (isInitiateRequest(requestType) ? "&partnerClientId=" + partnerClientId : "")
                + "&partnerCode=" + momoProperties.getPartnerCode()
                + "&redirectUrl=" + momoProperties.getRedirectUrl()
                + "&requestId=" + requestId
                + "&requestType=" + requestType;
        String signature = signHmacSha256(rawSignature, momoProperties.getSecretKey());

        Map<String, Object> payload = new HashMap<>();
        payload.put("partnerCode", momoProperties.getPartnerCode());
        payload.put("accessKey", momoProperties.getAccessKey());
        payload.put("partnerName", "Clothing");
        payload.put("storeId", "ClothingStore");
        payload.put("requestId", requestId);
        payload.put("amount", order.getTotalPrice());
        payload.put("orderId", orderId);
        payload.put("orderInfo", orderInfo);
        payload.put("redirectUrl", momoProperties.getRedirectUrl());
        payload.put("ipnUrl", momoProperties.getIpnUrl());
        payload.put("lang", "vi");
        payload.put("requestType", requestType);
        if (isInitiateRequest(requestType)) {
            payload.put("partnerClientId", partnerClientId);
        }
        payload.put("autoCapture", true);
        payload.put("extraData", extraData);
        payload.put("signature", signature);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<JsonNode> response;
        try {
            response = restTemplate.postForEntity(
                    momoProperties.getEndpoint(),
                    new HttpEntity<>(payload, headers),
                    JsonNode.class
            );
        } catch (HttpStatusCodeException ex) {
            throw mapMomoHttpError(ex);
        } catch (Exception ex) {
            log.error("Cannot call MoMo API", ex);
            throw new BusinessException("Failed to call MoMo API", HttpStatus.BAD_GATEWAY);
        }

        JsonNode body = response.getBody();
        if (body == null) {
            throw new BusinessException("Invalid MoMo response", HttpStatus.BAD_GATEWAY);
        }

        int resultCode = body.path("resultCode").asInt(-1);
        String payUrl = body.path("payUrl").asText("");
        if (resultCode != 0 || payUrl.isBlank()) {
            String msg = body.path("message").asText("MoMo create payment failed");
            throw new BusinessException(msg, HttpStatus.BAD_GATEWAY);
        }

        PaymentEntity payment = paymentRepository.findByOrderId(order.getId()).orElseGet(PaymentEntity::new);
        payment.setOrderId(order.getId());
        payment.setAmount(order.getTotalPrice());
        payment.setMethod(METHOD_MOMO);
        payment.setStatus(STATUS_CREATED);
        payment.setTransactionCode(orderId);
        if (payment.getCreatedAt() == null) {
            payment.setCreatedAt(LocalDateTime.now());
        }
        paymentRepository.save(payment);

        return payUrl;
    }

    @Override
    public void handleMomoIpn(Map<String, Object> payload) {
        if (payload == null) {
            return;
        }
        Object orderIdObj = payload.get("orderId");
        if (orderIdObj == null) {
            return;
        }
        String transactionCode = String.valueOf(orderIdObj);
        int resultCode = parseInt(payload.get("resultCode"), -1);

        PaymentEntity payment = paymentRepository.findByTransactionCode(transactionCode).orElse(null);
        if (payment == null) {
            return;
        }
        payment.setStatus(resultCode == 0 ? STATUS_PAID : STATUS_FAILED);
        paymentRepository.save(payment);
    }

    private void validateConfig() {
        if (isBlank(momoProperties.getEndpoint())
                || isBlank(momoProperties.getPartnerCode())
                || isBlank(momoProperties.getAccessKey())
                || isBlank(momoProperties.getSecretKey())
                || isBlank(momoProperties.getRedirectUrl())
                || isBlank(momoProperties.getIpnUrl())) {
            throw new BusinessException("MoMo config is incomplete", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String signHmacSha256(String data, String secretKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(rawHmac.length * 2);
            for (byte b : rawHmac) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new BusinessException("Cannot sign MoMo request", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private int parseInt(Object value, int fallback) {
        if (value == null) return fallback;
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return fallback;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isInitiateRequest(String requestType) {
        return "initiate".equalsIgnoreCase(requestType);
    }

    private BusinessException mapMomoHttpError(HttpStatusCodeException ex) {
        String body = ex.getResponseBodyAsString();
        try {
            JsonNode jsonNode = objectMapper.readTree(body);
            int resultCode = jsonNode.path("resultCode").asInt(-1);
            String message = jsonNode.path("message").asText("MoMo API error");
            log.warn("MoMo API rejected request. httpStatus={}, resultCode={}, message={}",
                    ex.getStatusCode(), resultCode, message);
            if (resultCode == 11007) {
                return new BusinessException(
                        "MoMo sai chữ ký (11007). Kiểm tra MOMO_PARTNER_CODE, MOMO_ACCESS_KEY, MOMO_SECRET_KEY và requestType.",
                        HttpStatus.BAD_GATEWAY
                );
            }
            return new BusinessException("MoMo error (" + resultCode + "): " + message, HttpStatus.BAD_GATEWAY);
        } catch (Exception parseEx) {
            log.warn("MoMo API error response cannot be parsed. httpStatus={}, body={}",
                    ex.getStatusCode(), body);
            return new BusinessException("MoMo API error: " + ex.getStatusCode(), HttpStatus.BAD_GATEWAY);
        }
    }
}
