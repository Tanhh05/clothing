package com.clothing.controller;

import com.clothing.service.OrderService;
import com.clothing.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;

    public PaymentController(PaymentService paymentService, OrderService orderService) {
        this.paymentService = paymentService;
        this.orderService = orderService;
    }

    @PostMapping("/momo/ipn")
    public ResponseEntity<Map<String, Object>> momoIpn(@RequestBody(required = false) Map<String, Object> payload) {
        paymentService.handleMomoIpn(payload);
        orderService.handleMomoPaymentIpn(payload);
        return ResponseEntity.ok(Map.of("resultCode", 0, "message", "success"));
    }

    @GetMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(@RequestParam Map<String, String> payload) {
        try {
            paymentService.handleVnpayIpn(payload);
            orderService.handleVnpayPaymentIpn(payload);
            return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
        } catch (RuntimeException ex) {
            return ResponseEntity.ok(Map.of("RspCode", "97", "Message", ex.getMessage()));
        }
    }
}
