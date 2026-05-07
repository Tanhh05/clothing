package com.clothing.service.impl;

import com.clothing.exception.BusinessException;
import com.clothing.service.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@Primary
@ConditionalOnExpression(
        "'${app.mail.brevo.api-key:}' != '' and '${app.mail.brevo.sender-email:}' != ''"
)
@Slf4j
public class BrevoEmailServiceImpl implements EmailService {

    private static final String BREVO_SEND_ENDPOINT = "https://api.brevo.com/v3/smtp/email";

    private final RestTemplate restTemplate = new RestTemplate();
    private final String brevoApiKey;
    private final String senderEmail;

    public BrevoEmailServiceImpl(
            @Value("${app.mail.brevo.api-key:}") String brevoApiKey,
            @Value("${app.mail.brevo.sender-email:}") String senderEmail
    ) {
        this.brevoApiKey = brevoApiKey == null ? "" : brevoApiKey.trim();
        this.senderEmail = senderEmail == null ? "" : senderEmail.trim();
    }

    @Override
    public void sendOrderConfirmationEmail(String toEmail, Long orderId) {
        sendEmail(
                toEmail,
                "Xac nhan don hang #" + orderId,
                "Don hang cua ban da duoc xac nhan. Ma don: " + orderId,
                false
        );
    }

    @Override
    public void sendOrderFailedEmail(String toEmail, Long orderId, String reason) {
        sendEmail(
                toEmail,
                "Don hang #" + orderId + " that bai",
                "Don hang cua ban khong thanh cong. Ly do: " + reason,
                false
        );
    }

    @Override
    public void sendPasswordResetOtpEmail(String toEmail, String fullName, String otpCode) {
        String receiverName = (fullName == null || fullName.isBlank()) ? "ban" : fullName.trim();
        String text = "Xin chao " + receiverName + ",\n\n"
                + "Ma OTP dat lai mat khau cua ban la: " + otpCode + "\n"
                + "Ma co hieu luc trong 5 phut.\n\n"
                + "Neu ban khong yeu cau dat lai mat khau, vui long bo qua email nay.\n";
        sendEmail(toEmail, "Ma OTP dat lai mat khau", text, false);
    }

    @Override
    public void sendTestEmail(String toEmail, String subject, String content) {
        sendEmail(toEmail, subject, content, true);
    }

    private void sendEmail(String toEmail, String subject, String textBody, boolean failOnError) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        Map<String, Object> payload = Map.of(
                "sender", Map.of("email", senderEmail),
                "to", List.of(Map.of("email", toEmail)),
                "subject", subject,
                "textContent", textBody
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    BREVO_SEND_ENDPOINT,
                    HttpMethod.POST,
                    entity,
                    JsonNode.class
            );
            if (!response.getStatusCode().is2xxSuccessful()) {
                String message = "Brevo send email failed";
                if (failOnError) {
                    throw new BusinessException(message, HttpStatus.BAD_GATEWAY);
                }
                log.warn("{} (non-blocking). toEmail={}, subject={}", message, toEmail, subject);
            }
        } catch (HttpStatusCodeException ex) {
            String body = ex.getResponseBodyAsString();
            String message = "Brevo API error (" + ex.getStatusCode() + "): " + (body == null ? ex.getMessage() : body);
            if (failOnError) {
                log.error("Brevo API error status={} body={}", ex.getStatusCode(), body);
                throw new BusinessException(message, HttpStatus.BAD_GATEWAY);
            }
            log.warn("Brevo non-blocking error. status={} toEmail={} subject={} body={}",
                    ex.getStatusCode(), toEmail, subject, body);
        } catch (Exception ex) {
            if (failOnError) {
                log.error("Failed to send email via Brevo API", ex);
                throw new BusinessException("Failed to send email via Brevo API", HttpStatus.BAD_GATEWAY);
            }
            log.warn("Failed to send email via Brevo API (non-blocking). toEmail={}, subject={}, message={}",
                    toEmail, subject, ex.getMessage());
        }
    }
}
