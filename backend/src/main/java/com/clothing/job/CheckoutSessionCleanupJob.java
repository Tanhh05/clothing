package com.clothing.job;

import com.clothing.repository.MomoCheckoutSessionRepository;
import com.clothing.repository.VnpayCheckoutSessionRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CheckoutSessionCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(CheckoutSessionCleanupJob.class);
    private static final String SESSION_STATUS_PENDING = "PENDING";

    private final MomoCheckoutSessionRepository momoCheckoutSessionRepository;
    private final VnpayCheckoutSessionRepository vnpayCheckoutSessionRepository;

    public CheckoutSessionCleanupJob(
            MomoCheckoutSessionRepository momoCheckoutSessionRepository,
            VnpayCheckoutSessionRepository vnpayCheckoutSessionRepository
    ) {
        this.momoCheckoutSessionRepository = momoCheckoutSessionRepository;
        this.vnpayCheckoutSessionRepository = vnpayCheckoutSessionRepository;
    }

    @Transactional
    @Scheduled(fixedDelayString = "${app.payment.cleanup-fixed-delay-ms:300000}")
    public void cleanupExpiredPendingSessions() {
        LocalDateTime now = LocalDateTime.now();
        long deletedMomo = momoCheckoutSessionRepository.deleteByStatusAndExpiresAtBefore(SESSION_STATUS_PENDING, now);
        long deletedVnpay = vnpayCheckoutSessionRepository.deleteByStatusAndExpiresAtBefore(SESSION_STATUS_PENDING, now);

        if (deletedMomo > 0 || deletedVnpay > 0) {
            log.info("Deleted expired checkout sessions. momo={}, vnpay={}", deletedMomo, deletedVnpay);
        }
    }
}
