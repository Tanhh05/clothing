package com.clothing.service.impl;

import com.clothing.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("sql-only")
public class LogEmailServiceImpl implements EmailService {

    @Override
    public void sendOrderConfirmationEmail(String toEmail, Long orderId) {
        log.info("Send order confirmation email to {} for order {}", toEmail, orderId);
    }

    @Override
    public void sendOrderFailedEmail(String toEmail, Long orderId, String reason) {
        log.info("Send order failed email to {} for order {} with reason {}", toEmail, orderId, reason);
    }

    @Override
    public void sendPasswordResetOtpEmail(String toEmail, String fullName, String otpCode) {
        log.info(
                "Send reset password OTP email to {} (name={}) with code {}",
                toEmail,
                fullName,
                otpCode
        );
    }

    @Override
    public void sendTestEmail(String toEmail, String subject, String content) {
        log.info("Send test email to {} with subject={} content={}", toEmail, subject, content);
    }
}
