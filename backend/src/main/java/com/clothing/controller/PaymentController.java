package com.clothing.controller;

import com.clothing.entity.OrderEntity;
import com.clothing.entity.PaymentEntity;
import com.clothing.repository.OrderRepository;
import com.clothing.repository.PaymentRepository;
import com.clothing.service.OrderService;
import com.clothing.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public PaymentController(
            PaymentService paymentService,
            OrderService orderService,
            PaymentRepository paymentRepository,
            OrderRepository orderRepository
    ) {
        this.paymentService = paymentService;
        this.orderService = orderService;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
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
}
