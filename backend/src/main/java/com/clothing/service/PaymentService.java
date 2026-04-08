package com.clothing.service;

import com.clothing.entity.OrderEntity;

import java.util.Map;

public interface PaymentService {

    String createMomoPayment(OrderEntity order);

    void handleMomoIpn(Map<String, Object> payload);
}
