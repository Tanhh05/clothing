package com.clothing.service.impl;

import com.clothing.service.EmailService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnExpression(
        "'${spring.mail.username:}' != '' and '${spring.mail.password:}' != ''"
)
public class SmtpEmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public SmtpEmailServiceImpl(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String fromEmail
    ) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail == null ? "" : fromEmail.trim();
    }

    @Override
    public void sendOrderConfirmationEmail(String toEmail, Long orderId) {
        sendSimpleMail(
                toEmail,
                "Xac nhan don hang #" + orderId,
                "Don hang cua ban da duoc xac nhan. Ma don: " + orderId
        );
    }

    @Override
    public void sendOrderFailedEmail(String toEmail, Long orderId, String reason) {
        sendSimpleMail(
                toEmail,
                "Don hang #" + orderId + " that bai",
                "Don hang cua ban khong thanh cong. Ly do: " + reason
        );
    }

    @Override
    public void sendPasswordResetOtpEmail(String toEmail, String fullName, String otpCode) {
        String receiverName = (fullName == null || fullName.isBlank()) ? "ban" : fullName.trim();
        String body = "Xin chao " + receiverName + ",\n\n"
                + "Ma OTP dat lai mat khau cua ban la: " + otpCode + "\n"
                + "Ma co hieu luc trong 5 phut.\n\n"
                + "Neu ban khong yeu cau dat lai mat khau, vui long bo qua email nay.\n";
        sendSimpleMail(toEmail, "Ma OTP dat lai mat khau", body);
    }

    @Override
    public void sendTestEmail(String toEmail, String subject, String content) {
        sendSimpleMail(toEmail, subject, content);
    }

    private void sendSimpleMail(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (!fromEmail.isBlank()) {
            message.setFrom(fromEmail);
        }
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
