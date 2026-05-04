package com.clothing.service.impl;

import com.clothing.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LogEmailServiceImpl implements EmailService {

    @Override
    public void sendOrderConfirmationEmail(String toEmail, Long orderId) {
        log.info("Send order confirmation email to {} for order {}", toEmail, orderId);
    }

    @Override
    public void sendOrderFailedEmail(String toEmail, Long orderId, String reason) {
        log.info("Send order failed email to {} for order {} with reason {}", toEmail, orderId, reason);
    }
}
