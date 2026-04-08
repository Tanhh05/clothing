package com.clothing.messaging.listener;

import com.clothing.messaging.event.OrderCreatedEvent;
import com.clothing.service.OrderAsyncProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderEventListener {

    private final OrderAsyncProcessingService orderAsyncProcessingService;

    public OrderEventListener(OrderAsyncProcessingService orderAsyncProcessingService) {
        this.orderAsyncProcessingService = orderAsyncProcessingService;
    }

    @RabbitListener(queues = "${app.messaging.order.queue:order.processing.queue}")
    public void onOrderCreated(OrderCreatedEvent event) {
        if (event == null || event.getOrderId() == null) {
            return;
        }
        try {
            orderAsyncProcessingService.processOrderCreated(event.getOrderId());
        } catch (Exception ex) {
            // Avoid endless requeue loop. Marking and retries can be added via DLQ policy later.
            log.error("Failed to process order async event for orderId={}", event.getOrderId(), ex);
        }
    }
}
