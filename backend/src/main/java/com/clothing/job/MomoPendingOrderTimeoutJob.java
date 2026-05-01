package com.clothing.job;

import com.clothing.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MomoPendingOrderTimeoutJob {

    private static final Logger log = LoggerFactory.getLogger(MomoPendingOrderTimeoutJob.class);

    private final OrderService orderService;

    public MomoPendingOrderTimeoutJob(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(fixedDelayString = "${app.payment.momo-order-timeout-check-fixed-delay-ms:60000}")
    public void cancelExpiredMomoWaitingOrders() {
        int momoCancelled = orderService.cancelExpiredMomoWaitingPaymentOrders();
        if (momoCancelled > 0) {
            log.info("Auto-cancelled {} expired MoMo waiting-payment orders", momoCancelled);
        }

        int codCancelled = orderService.cancelExpiredCodReservedOrders();
        if (codCancelled > 0) {
            log.info("Auto-cancelled {} expired COD reserved orders", codCancelled);
        }
    }
}
