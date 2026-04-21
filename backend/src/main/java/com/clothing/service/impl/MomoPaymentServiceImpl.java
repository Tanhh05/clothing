package com.clothing.service.impl;

import com.clothing.config.MomoProperties;
import com.clothing.config.VnpayProperties;
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
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class MomoPaymentServiceImpl implements PaymentService {
    private static final Logger log = LoggerFactory.getLogger(MomoPaymentServiceImpl.class);

    private static final String METHOD_MOMO = "MOMO";
    private static final String METHOD_VNPAY = "VNPAY";
    private static final String STATUS_CREATED = "CREATED";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_FAILED = "FAILED";
    private static final DateTimeFormatter VNPAY_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Set<String> ALLOWED_REQUEST_TYPES = Set.of(
            "captureWallet",
            "payWithATM",
            "payWithCC"
    );

    private final RestTemplate restTemplate;
    private final MomoProperties momoProperties;
    private final VnpayProperties vnpayProperties;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;

    public MomoPaymentServiceImpl(
            MomoProperties momoProperties,
            VnpayProperties vnpayProperties,
            PaymentRepository paymentRepository
    ) {
        this.restTemplate = new RestTemplate();
        this.momoProperties = momoProperties;
        this.vnpayProperties = vnpayProperties;
        this.paymentRepository = paymentRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String createMomoPayment(OrderEntity order, String requestTypeOverride) {
        if (order == null || order.getId() == null) {
            throw new BusinessException("Order is required to create MoMo payment", HttpStatus.BAD_REQUEST);
        }
        if (order.getTotalPrice() == null || order.getTotalPrice() <= 0) {
            throw new BusinessException("Order amount must be > 0", HttpStatus.BAD_REQUEST);
        }
        String orderInfo = "Thanh toan don hang #" + order.getId();
        String extraData = Base64.getEncoder().encodeToString(("orderId=" + order.getId()).getBytes(StandardCharsets.UTF_8));
        return createMomoPaymentInternal(
                "ORD" + order.getId(),
                order.getId(),
                order.getTotalPrice(),
                orderInfo,
                extraData,
                requestTypeOverride
        );
    }

    @Override
    public String createMomoPaymentForSession(
            String sessionToken,
            Long amount,
            String orderInfo,
            String extraData,
            String requestTypeOverride
    ) {
        if (isBlank(sessionToken)) {
            throw new BusinessException("sessionToken is required", HttpStatus.BAD_REQUEST);
        }
        if (amount == null || amount <= 0) {
            throw new BusinessException("amount must be > 0", HttpStatus.BAD_REQUEST);
        }
        String safeOrderInfo = isBlank(orderInfo) ? "Thanh toan don hang tam" : orderInfo.trim();
        String safeExtraData = extraData == null ? "" : extraData;
        return createMomoPaymentInternal(
                "TMP" + sessionToken.trim(),
                null,
                amount,
                safeOrderInfo,
                safeExtraData,
                requestTypeOverride
        );
    }

    private String createMomoPaymentInternal(
            String reference,
            Long orderIdForPayment,
            Long amountValue,
            String orderInfo,
            String extraData,
            String requestTypeOverride
    ) {
        validateConfig();

        String normalizedRef = isBlank(reference) ? "TMP" : reference.trim();
        String orderId = normalizedRef + "-" + System.currentTimeMillis();
        String requestId = "REQ" + normalizedRef + "-" + System.currentTimeMillis();
        String amount = String.valueOf(amountValue);
        String requestType = resolveRequestType(requestTypeOverride);
        String partnerClientId = "";

        Map<String, Object> payload = new HashMap<>();
        payload.put("partnerCode", momoProperties.getPartnerCode());
        payload.put("accessKey", momoProperties.getAccessKey());
        payload.put("partnerName", "Clothing");
        payload.put("storeId", "ClothingStore");
        payload.put("requestId", requestId);
        payload.put("amount", amountValue);
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

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<JsonNode> response = callMomoCreateWithSignatureFallback(
                payload,
                headers,
                amount,
                extraData,
                orderId,
                orderInfo,
                requestId,
                requestType,
                partnerClientId
        );

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

        PaymentEntity payment;
        if (orderIdForPayment != null) {
            payment = paymentRepository.findByOrderId(orderIdForPayment).orElseGet(PaymentEntity::new);
        } else {
            payment = new PaymentEntity();
        }
        payment.setOrderId(orderIdForPayment);
        payment.setAmount(amountValue);
        payment.setMethod(METHOD_MOMO);
        payment.setStatus(STATUS_CREATED);
        payment.setTransactionCode(orderId);
        if (payment.getCreatedAt() == null) {
            payment.setCreatedAt(LocalDateTime.now());
        }
        paymentRepository.save(payment);

        return payUrl;
    }

    private ResponseEntity<JsonNode> callMomoCreateWithSignatureFallback(
            Map<String, Object> payload,
            HttpHeaders headers,
            String amount,
            String extraData,
            String orderId,
            String orderInfo,
            String requestId,
            String requestType,
            String partnerClientId
    ) {
        List<String> rawSignatures = buildCreateSignatureCandidates(
                amount,
                extraData,
                orderId,
                orderInfo,
                requestId,
                requestType,
                partnerClientId
        );
        BusinessException lastError = null;
        for (String rawSignature : rawSignatures) {
            payload.put("signature", signHmacSha256(rawSignature, momoProperties.getSecretKey()));
            try {
                ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                        momoProperties.getEndpoint(),
                        new HttpEntity<>(payload, headers),
                        JsonNode.class
                );
                JsonNode body = response.getBody();
                if (body != null && body.path("resultCode").asInt(0) == 11007) {
                    lastError = new BusinessException(
                            "MoMo sai chữ ký (11007). Kiểm tra MOMO_PARTNER_CODE, MOMO_ACCESS_KEY, MOMO_SECRET_KEY và requestType.",
                            HttpStatus.BAD_GATEWAY
                    );
                    continue;
                }
                return response;
            } catch (HttpStatusCodeException ex) {
                BusinessException mapped = mapMomoHttpError(ex);
                if (mapped.getMessage().contains("(11007)")) {
                    lastError = mapped;
                    continue;
                }
                throw mapped;
            } catch (Exception ex) {
                log.error("Cannot call MoMo API", ex);
                throw new BusinessException("Failed to call MoMo API", HttpStatus.BAD_GATEWAY);
            }
        }
        throw lastError == null
                ? new BusinessException("Failed to call MoMo API", HttpStatus.BAD_GATEWAY)
                : lastError;
    }

    private List<String> buildCreateSignatureCandidates(
            String amount,
            String extraData,
            String orderId,
            String orderInfo,
            String requestId,
            String requestType,
            String partnerClientId
    ) {
        ArrayList<String> candidates = new ArrayList<>();
        String common = "amount=" + amount
                + "&extraData=" + extraData
                + "&ipnUrl=" + momoProperties.getIpnUrl()
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + momoProperties.getPartnerCode()
                + "&redirectUrl=" + momoProperties.getRedirectUrl()
                + "&requestId=" + requestId
                + "&requestType=" + requestType;

        // Legacy format (commonly used in many MoMo v2 examples).
        String legacy = "accessKey=" + momoProperties.getAccessKey() + "&" + common
                + (isInitiateRequest(requestType) ? "&partnerClientId=" + partnerClientId : "");
        candidates.add(legacy);

        // Some integrations validate signature without `accessKey`.
        String noAccessKey = common + (isInitiateRequest(requestType) ? "&partnerClientId=" + partnerClientId : "");
        candidates.add(noAccessKey);

        return candidates;
    }

    @Override
    public void handleMomoIpn(Map<String, Object> payload) {
        if (payload == null) {
            return;
        }
        verifyMomoIpnSignature(payload);

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

    @Override
    public String createVnpayPaymentForSession(String sessionToken, Long amount, String orderInfo, String bankCode) {
        if (isBlank(sessionToken)) {
            throw new BusinessException("sessionToken is required", HttpStatus.BAD_REQUEST);
        }
        if (amount == null || amount <= 0) {
            throw new BusinessException("amount must be > 0", HttpStatus.BAD_REQUEST);
        }
        validateVnpayConfig();

        String txnRef = "TMP" + sessionToken.trim() + "-" + System.currentTimeMillis();
        String info = isBlank(orderInfo) ? "Thanh toan don hang tam" : orderInfo.trim();
        String requestUrl = buildVnpayPaymentUrl(txnRef, amount, info, bankCode);

        PaymentEntity payment = new PaymentEntity();
        payment.setOrderId(null);
        payment.setAmount(amount);
        payment.setMethod(METHOD_VNPAY);
        payment.setStatus(STATUS_CREATED);
        payment.setTransactionCode(txnRef);
        payment.setCreatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        return requestUrl;
    }

    @Override
    public void handleVnpayIpn(Map<String, String> payload) {
        if (payload == null || payload.isEmpty()) {
            return;
        }
        verifyVnpayIpnSignature(payload);

        String txnRef = safeTrim(payload.get("vnp_TxnRef"));
        if (txnRef.isBlank()) {
            return;
        }
        PaymentEntity payment = paymentRepository.findByTransactionCode(txnRef).orElse(null);
        if (payment == null) {
            return;
        }

        String responseCode = safeTrim(payload.get("vnp_ResponseCode"));
        String txnStatus = safeTrim(payload.get("vnp_TransactionStatus"));
        boolean success = "00".equals(responseCode) && (txnStatus.isBlank() || "00".equals(txnStatus));
        payment.setStatus(success ? STATUS_PAID : STATUS_FAILED);
        paymentRepository.save(payment);
    }

    private void verifyMomoIpnSignature(Map<String, Object> payload) {
        String expectedSignature = toText(payload.get("signature"));
        if (expectedSignature.isBlank()) {
            throw new BusinessException("Missing MoMo IPN signature", HttpStatus.BAD_REQUEST);
        }
        if (isBlank(momoProperties.getSecretKey())) {
            throw new BusinessException("MoMo secret key is not configured", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (!isBlank(momoProperties.getPartnerCode())) {
            String partnerCode = toText(payload.get("partnerCode"));
            if (!partnerCode.isBlank() && !momoProperties.getPartnerCode().equals(partnerCode)) {
                throw new BusinessException("Invalid MoMo partnerCode", HttpStatus.BAD_REQUEST);
            }
        }

        boolean matched = buildMomoIpnSignatureCandidates(payload).stream()
                .map(raw -> signHmacSha256(raw, momoProperties.getSecretKey()))
                .anyMatch(sig -> sig.equalsIgnoreCase(expectedSignature));
        if (!matched) {
            throw new BusinessException("Invalid MoMo IPN signature", HttpStatus.BAD_REQUEST);
        }
    }

    private java.util.List<String> buildMomoIpnSignatureCandidates(Map<String, Object> payload) {
        ArrayList<String> candidates = new ArrayList<>();

        // Common MoMo callback raw signature format for v2 gateway.
        LinkedHashMap<String, String> strict = new LinkedHashMap<>();
        strict.put("accessKey", momoProperties.getAccessKey());
        strict.put("amount", toText(payload.get("amount")));
        strict.put("extraData", toText(payload.get("extraData")));
        strict.put("message", toText(payload.get("message")));
        strict.put("orderId", toText(payload.get("orderId")));
        strict.put("orderInfo", toText(payload.get("orderInfo")));
        strict.put("orderType", toText(payload.get("orderType")));
        strict.put("partnerCode", toText(payload.get("partnerCode")));
        strict.put("payType", toText(payload.get("payType")));
        strict.put("requestId", toText(payload.get("requestId")));
        strict.put("responseTime", toText(payload.get("responseTime")));
        strict.put("resultCode", toText(payload.get("resultCode")));
        strict.put("transId", toText(payload.get("transId")));
        candidates.add(joinAsRawData(strict));

        // Compatibility candidate: without accessKey in callback payload.
        LinkedHashMap<String, String> withoutAccessKey = new LinkedHashMap<>(strict);
        withoutAccessKey.remove("accessKey");
        candidates.add(joinAsRawData(withoutAccessKey));

        // Fallback candidate: sign sorted callback fields except signature itself.
        TreeMap<String, String> sortedFields = new TreeMap<>();
        Set<String> ignored = Set.of("signature");
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            String key = entry.getKey();
            if (key == null || ignored.contains(key)) {
                continue;
            }
            sortedFields.put(key, toText(entry.getValue()));
        }
        if (!sortedFields.isEmpty()) {
            candidates.add(joinAsRawData(sortedFields));
        }

        return candidates;
    }

    private String joinAsRawData(Map<String, String> fields) {
        return fields.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + (entry.getValue() == null ? "" : entry.getValue()))
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
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

    private void validateVnpayConfig() {
        if (isBlank(vnpayProperties.getEndpoint())
                || isBlank(vnpayProperties.getTmnCode())
                || isBlank(vnpayProperties.getHashSecret())
                || isBlank(vnpayProperties.getReturnUrl())
                || isBlank(vnpayProperties.getIpnUrl())) {
            throw new BusinessException("VNPAY config is incomplete", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String buildVnpayPaymentUrl(String txnRef, Long amount, String orderInfo, String bankCode) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDateTime expireAt = now.plusMinutes(Math.max(1, vnpayProperties.getExpireMinutes()));

        TreeMap<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", vnpayProperties.getTmnCode().trim());
        params.put("vnp_Amount", String.valueOf(amount * 100));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", orderInfo);
        params.put("vnp_OrderType", safeTrim(vnpayProperties.getOrderType()).isBlank() ? "other" : vnpayProperties.getOrderType().trim());
        params.put("vnp_Locale", safeTrim(vnpayProperties.getLocale()).isBlank() ? "vn" : vnpayProperties.getLocale().trim());
        params.put("vnp_ReturnUrl", vnpayProperties.getReturnUrl().trim());
        params.put("vnp_IpAddr", safeTrim(vnpayProperties.getIpAddress()).isBlank() ? "127.0.0.1" : vnpayProperties.getIpAddress().trim());
        params.put("vnp_CreateDate", now.format(VNPAY_TIME_FORMAT));
        params.put("vnp_ExpireDate", expireAt.format(VNPAY_TIME_FORMAT));
        String bank = safeTrim(bankCode).toUpperCase(Locale.ROOT);
        if (!bank.isBlank()) {
            params.put("vnp_BankCode", bank);
        }

        String hashData = buildVnpaySigningData(params);
        String secureHash = signHmacSha512(hashData, vnpayProperties.getHashSecret());

        String query = params.entrySet().stream()
                .map(entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()))
                .collect(Collectors.joining("&"));
        return vnpayProperties.getEndpoint().trim()
                + "?" + query
                + "&vnp_SecureHash=" + secureHash;
    }

    private void verifyVnpayIpnSignature(Map<String, String> payload) {
        String expectedSignature = safeTrim(payload.get("vnp_SecureHash"));
        if (expectedSignature.isBlank()) {
            throw new BusinessException("Missing VNPAY secure hash", HttpStatus.BAD_REQUEST);
        }
        if (isBlank(vnpayProperties.getHashSecret())) {
            throw new BusinessException("VNPAY hash secret is not configured", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        TreeMap<String, String> fields = new TreeMap<>();
        for (Map.Entry<String, String> entry : payload.entrySet()) {
            String key = entry.getKey();
            if (isBlank(key)
                    || "vnp_SecureHash".equalsIgnoreCase(key)
                    || "vnp_SecureHashType".equalsIgnoreCase(key)) {
                continue;
            }
            fields.put(key, safeTrim(entry.getValue()));
        }

        String rawData = buildVnpaySigningData(fields);
        String signed = signHmacSha512(rawData, vnpayProperties.getHashSecret());
        if (!signed.equalsIgnoreCase(expectedSignature)) {
            throw new BusinessException("Invalid VNPAY signature", HttpStatus.BAD_REQUEST);
        }
    }

    private String buildVnpaySigningData(Map<String, String> params) {
        return params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + urlEncode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String resolveRequestType(String requestTypeOverride) {
        String configured = normalizeRequestType(momoProperties.getRequestType());
        String requested = normalizeRequestType(requestTypeOverride);
        String selected = requested == null ? configured : requested;
        if (selected == null) {
            throw new BusinessException(
                    "MoMo requestType không hợp lệ. Hỗ trợ: captureWallet, payWithATM, payWithCC",
                    HttpStatus.BAD_REQUEST
            );
        }
        return selected;
    }

    private String normalizeRequestType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        for (String allowed : ALLOWED_REQUEST_TYPES) {
            if (allowed.equalsIgnoreCase(normalized)) {
                return allowed;
            }
        }
        return null;
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

    private String signHmacSha512(String data, String secretKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(rawHmac.length * 2);
            for (byte b : rawHmac) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new BusinessException("Cannot sign VNPAY request", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20");
        } catch (Exception ex) {
            throw new BusinessException("Cannot encode VNPAY payload", HttpStatus.INTERNAL_SERVER_ERROR);
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

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String toText(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
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
