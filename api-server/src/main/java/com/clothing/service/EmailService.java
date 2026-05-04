package com.clothing.service;

public interface EmailService {

    void sendOrderConfirmationEmail(String toEmail, Long orderId);

    void sendOrderFailedEmail(String toEmail, Long orderId, String reason);

    void sendPasswordResetOtpEmail(String toEmail, String fullName, String otpCode);

    void sendTestEmail(String toEmail, String subject, String content);
}
