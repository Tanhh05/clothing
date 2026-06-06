package com.clothing.service;

import com.clothing.entity.OrderEntity;

import java.util.Map;

public interface PaymentService {

    default String createMomoPayment(OrderEntity order) {
        return createMomoPayment(order, null);
    }

    String createMomoPayment(OrderEntity order, String requestTypeOverride);

    String createMomoPaymentForSession(
            String sessionToken,
            Long amount,
            String orderInfo,
            String extraData,
            String requestTypeOverride
    );

    void handleMomoIpn(Map<String, Object> payload);

    String createVnpayPaymentForSession(
            String sessionToken,
            Long amount,
            String orderInfo,
            String bankCode
    );

    PaymentNotificationResult handleVnpayIpn(Map<String, String> payload);

    Map<String, Object> verifyPaymentReturn(String gateway, Map<String, String> payload);
}
