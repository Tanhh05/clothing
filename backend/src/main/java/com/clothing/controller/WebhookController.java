package com.clothing.controller;

import com.clothing.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private final OrderService orderService;

    public WebhookController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/ghn")
    public ResponseEntity<Map<String, Object>> ghnWebhook(@RequestBody(required = false) Map<String, Object> payload) {
        orderService.handleGhnWebhook(payload);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
