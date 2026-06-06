package com.clothing.service.impl;

import com.clothing.config.MomoProperties;
import com.clothing.config.VnpayProperties;
import com.clothing.entity.OrderEntity;
import com.clothing.entity.PaymentEntity;
import com.clothing.exception.BusinessException;
import com.clothing.repository.PaymentRepository;
import com.clothing.service.PaymentNotificationResult;
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
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10_000);
        requestFactory.setReadTimeout(30_000);
        this.restTemplate = new RestTemplate(requestFactory);
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
        PaymentEntity existingPayment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        if (existingPayment != null
                && !STATUS_PAID.equalsIgnoreCase(String.valueOf(existingPayment.getStatus()))
                && !STATUS_FAILED.equalsIgnoreCase(String.valueOf(existingPayment.getStatus()))) {
            throw new BusinessException(
                    "Đơn hàng này đã có mã QR thanh toán đang chờ xử lý",
                    HttpStatus.BAD_REQUEST
            );
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

        String rawSignature = buildMomoCreateSignature(
                amount, extraData, orderId, orderInfo, requestId, requestType
        );
        payload.put("signature", signHmacSha256(rawSignature, momoProperties.getSecretKey()));
        ResponseEntity<JsonNode> response = callMomoCreate(payload, headers);

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

    private ResponseEntity<JsonNode> callMomoCreate(Map<String, Object> payload, HttpHeaders headers) {
        try {
            return restTemplate.postForEntity(
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
    }

    private String buildMomoCreateSignature(
            String amount,
            String extraData,
            String orderId,
            String orderInfo,
            String requestId,
            String requestType
    ) {
        return "accessKey=" + momoProperties.getAccessKey()
                + "&amount=" + amount
                + "&extraData=" + extraData
                + "&ipnUrl=" + momoProperties.getIpnUrl()
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + momoProperties.getPartnerCode()
                + "&redirectUrl=" + momoProperties.getRedirectUrl()
                + "&requestId=" + requestId
                + "&requestType=" + requestType;
    }

    @Override
    public void handleMomoIpn(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            throw new BusinessException("MoMo payload is required", HttpStatus.BAD_REQUEST);
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
            throw new BusinessException("MoMo transaction not found", HttpStatus.NOT_FOUND);
        }
        long callbackAmount = parseLong(payload.get("amount"), -1L);
        if (callbackAmount < 0 || payment.getAmount() == null || callbackAmount != payment.getAmount()) {
            throw new BusinessException("MoMo payment amount does not match", HttpStatus.BAD_REQUEST);
        }
        if (STATUS_PAID.equalsIgnoreCase(safeTrim(payment.getStatus()))) {
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
    public PaymentNotificationResult handleVnpayIpn(Map<String, String> payload) {
        if (payload == null || payload.isEmpty()) {
            throw new BusinessException("VNPAY payload is required", HttpStatus.BAD_REQUEST);
        }
        verifyVnpayIpnSignature(payload);

        String txnRef = safeTrim(payload.get("vnp_TxnRef"));
        if (txnRef.isBlank()) {
            return PaymentNotificationResult.notFound();
        }
        PaymentEntity payment = paymentRepository.findByTransactionCode(txnRef).orElse(null);
        if (payment == null) {
            return PaymentNotificationResult.notFound();
        }
        long callbackAmount = parseLong(payload.get("vnp_Amount"), -1L);
        if (payment.getAmount() == null || callbackAmount != payment.getAmount() * 100L) {
            return PaymentNotificationResult.invalidAmount();
        }

        String responseCode = safeTrim(payload.get("vnp_ResponseCode"));
        String txnStatus = safeTrim(payload.get("vnp_TransactionStatus"));
        boolean success = "00".equals(responseCode) && (txnStatus.isBlank() || "00".equals(txnStatus));
        boolean alreadyProcessed = STATUS_PAID.equalsIgnoreCase(safeTrim(payment.getStatus()));
        if (alreadyProcessed) {
            return PaymentNotificationResult.alreadyProcessed(true);
        }
        payment.setStatus(success ? STATUS_PAID : STATUS_FAILED);
        paymentRepository.save(payment);
        return PaymentNotificationResult.success(success);
    }

    @Override
    public Map<String, Object> verifyPaymentReturn(String gateway, Map<String, String> payload) {
        String normalizedGateway = safeTrim(gateway).toUpperCase(Locale.ROOT);
        String transactionCode;
        boolean gatewaySuccessful;
        long callbackAmount;
        boolean amountMatches;
        if (METHOD_MOMO.equals(normalizedGateway)) {
            Map<String, Object> momoPayload = new LinkedHashMap<>();
            payload.forEach(momoPayload::put);
            verifyMomoIpnSignature(momoPayload);
            transactionCode = safeTrim(payload.get("orderId"));
            gatewaySuccessful = parseInt(payload.get("resultCode"), -1) == 0;
            callbackAmount = parseLong(payload.get("amount"), -1L);
            amountMatches = callbackAmount >= 0;
        } else if (METHOD_VNPAY.equals(normalizedGateway)) {
            verifyVnpayIpnSignature(payload);
            transactionCode = safeTrim(payload.get("vnp_TxnRef"));
            gatewaySuccessful = "00".equals(safeTrim(payload.get("vnp_ResponseCode")))
                    && "00".equals(safeTrim(payload.get("vnp_TransactionStatus")));
            callbackAmount = parseLong(payload.get("vnp_Amount"), -1L);
            amountMatches = callbackAmount >= 0 && callbackAmount % 100L == 0;
        } else {
            throw new BusinessException("Unsupported payment gateway", HttpStatus.BAD_REQUEST);
        }

        PaymentEntity payment = paymentRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new BusinessException("Payment transaction not found", HttpStatus.NOT_FOUND));
        if (METHOD_VNPAY.equals(normalizedGateway)) {
            amountMatches = amountMatches
                    && payment.getAmount() != null
                    && callbackAmount == payment.getAmount() * 100L;
        } else {
            amountMatches = amountMatches
                    && payment.getAmount() != null
                    && callbackAmount == payment.getAmount();
        }
        if (!amountMatches) {
            throw new BusinessException("Payment amount does not match", HttpStatus.BAD_REQUEST);
        }
        String status = safeTrim(payment.getStatus()).toUpperCase(Locale.ROOT);
        return Map.of(
                "gateway", normalizedGateway,
                "transactionCode", transactionCode,
                "status", status,
                "paid", STATUS_PAID.equals(status),
                "gatewaySuccessful", gatewaySuccessful
        );
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

        String rawSignature = buildMomoIpnSignature(payload);
        String actualSignature = signHmacSha256(rawSignature, momoProperties.getSecretKey());
        if (!actualSignature.equalsIgnoreCase(expectedSignature)) {
            throw new BusinessException("Invalid MoMo IPN signature", HttpStatus.BAD_REQUEST);
        }
    }

    private String buildMomoIpnSignature(Map<String, Object> payload) {
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
        return joinAsRawData(strict);
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
        String tmnCode = safeTrim(payload.get("vnp_TmnCode"));
        if (!isBlank(vnpayProperties.getTmnCode())
                && !tmnCode.isBlank()
                && !vnpayProperties.getTmnCode().equals(tmnCode)) {
            throw new BusinessException("Invalid VNPAY merchant code", HttpStatus.BAD_REQUEST);
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

    private long parseLong(Object value, long fallback) {
        if (value == null) return fallback;
        try {
            return Long.parseLong(String.valueOf(value));
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
