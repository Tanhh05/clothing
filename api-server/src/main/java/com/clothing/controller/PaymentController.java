package com.clothing.controller;

import com.clothing.entity.OrderEntity;
import com.clothing.entity.PaymentEntity;
import com.clothing.entity.MomoIpnLogEntity;
import com.clothing.repository.MomoIpnLogRepository;
import com.clothing.repository.OrderRepository;
import com.clothing.repository.PaymentRepository;
import com.clothing.service.OrderService;
import com.clothing.service.PaymentNotificationResult;
import com.clothing.service.PaymentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final MomoIpnLogRepository momoIpnLogRepository;
    private final ObjectMapper objectMapper;

    public PaymentController(
            PaymentService paymentService,
            OrderService orderService,
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            MomoIpnLogRepository momoIpnLogRepository
    ) {
        this.paymentService = paymentService;
        this.orderService = orderService;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.momoIpnLogRepository = momoIpnLogRepository;
        this.objectMapper = new ObjectMapper();
    }

    @PostMapping("/momo/ipn")
    public ResponseEntity<Void> momoIpn(@RequestBody(required = false) Map<String, Object> payload) {
        MomoIpnLogEntity log = createMomoIpnLog(payload);
        try {
            paymentService.handleMomoIpn(payload);
            orderService.handleMomoPaymentIpn(payload);
            markMomoIpnLog(log, "SUCCESS", "Processed successfully");
            return ResponseEntity.noContent().build();
        } catch (RuntimeException ex) {
            markMomoIpnLog(log, "FAILED", ex.getMessage());
            throw ex;
        }
    }

    @GetMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(@RequestParam Map<String, String> payload) {
        if (payload == null || payload.isEmpty()) {
            return ResponseEntity.ok(Map.of("RspCode", "99", "Message", "Invalid request"));
        }
        try {
            PaymentNotificationResult result = paymentService.handleVnpayIpn(payload);
            if (result.paymentSuccessful()) {
                orderService.handleVnpayPaymentIpn(payload);
            }
            return ResponseEntity.ok(Map.of(
                    "RspCode", result.responseCode(),
                    "Message", result.message()
            ));
        } catch (RuntimeException ex) {
            return ResponseEntity.ok(Map.of("RspCode", "97", "Message", ex.getMessage()));
        }
    }

    @GetMapping("/return/status")
    public ResponseEntity<Map<String, Object>> verifyPaymentReturn(
            @RequestParam String gateway,
            @RequestParam Map<String, String> payload
    ) {
        Map<String, String> gatewayPayload = new LinkedHashMap<>(payload);
        gatewayPayload.remove("gateway");
        return ResponseEntity.ok(paymentService.verifyPaymentReturn(gateway, gatewayPayload));
    }

    @GetMapping("/orders/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getOrderPaymentStatus(@PathVariable Long orderId) {
        PaymentEntity payment = paymentRepository.findByOrderId(orderId).orElse(null);
        OrderEntity order = orderRepository.findById(orderId).orElse(null);
        String paymentStatus = payment == null ? "UNPAID" : String.valueOf(payment.getStatus());
        String orderStatus = order == null ? "" : String.valueOf(order.getStatus());
        boolean paid = "PAID".equalsIgnoreCase(paymentStatus);
        return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "paymentStatus", paymentStatus,
                "paid", paid,
                "orderStatus", orderStatus
        ));
    }

    @GetMapping("/momo/ipn-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getMomoIpnLogs(
            @RequestParam(required = false) String orderId
    ) {
        List<MomoIpnLogEntity> logs = (orderId == null || orderId.isBlank())
                ? momoIpnLogRepository.findTop200ByOrderByIdDesc()
                : momoIpnLogRepository.findTop200ByOrderIdOrderByIdDesc(orderId.trim());

        List<Map<String, Object>> response = logs.stream().map(log -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", log.getId());
            row.put("orderId", safe(log.getOrderId()));
            row.put("requestId", safe(log.getRequestId()));
            row.put("transId", safe(log.getTransId()));
            row.put("resultCode", log.getResultCode());
            row.put("message", safe(log.getMessage()));
            row.put("processStatus", safe(log.getProcessStatus()));
            row.put("processMessage", safe(log.getProcessMessage()));
            row.put("createdAt", log.getCreatedAt());
            row.put("updatedAt", log.getUpdatedAt());
            row.put("rawPayload", safe(log.getRawPayload()));
            return row;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    private MomoIpnLogEntity createMomoIpnLog(Map<String, Object> payload) {
        MomoIpnLogEntity log = new MomoIpnLogEntity();
        log.setOrderId(readText(payload, "orderId"));
        log.setRequestId(readText(payload, "requestId"));
        log.setTransId(readText(payload, "transId"));
        log.setResultCode(readInt(payload, "resultCode"));
        log.setMessage(readText(payload, "message"));
        log.setRawPayload(toJson(payload));
        log.setProcessStatus("RECEIVED");
        log.setProcessMessage("Received callback");
        log.setCreatedAt(LocalDateTime.now());
        log.setUpdatedAt(LocalDateTime.now());
        return momoIpnLogRepository.save(log);
    }

    private void markMomoIpnLog(MomoIpnLogEntity log, String status, String message) {
        if (log == null) return;
        log.setProcessStatus(status);
        log.setProcessMessage(safe(message));
        log.setUpdatedAt(LocalDateTime.now());
        momoIpnLogRepository.save(log);
    }

    private String readText(Map<String, Object> payload, String key) {
        if (payload == null) return "";
        Object value = payload.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private Integer readInt(Map<String, Object> payload, String key) {
        if (payload == null) return null;
        Object value = payload.get(key);
        if (value == null) return null;
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String toJson(Map<String, Object> payload) {
        if (payload == null) return "{}";
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return String.valueOf(payload);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
