package com.clothing.controller;

import com.clothing.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/momo/ipn")
    public ResponseEntity<Map<String, Object>> momoIpn(@RequestBody(required = false) Map<String, Object> payload) {
        paymentService.handleMomoIpn(payload);
        return ResponseEntity.ok(Map.of("resultCode", 0, "message", "success"));
    }
}
