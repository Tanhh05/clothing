package com.clothing.messaging.publisher;

import com.clothing.service.OrderAsyncProcessingService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.integrations.rabbit", name = "enabled", havingValue = "false")
public class LocalOrderEventPublisher implements OrderEventPublisher {

    private final OrderAsyncProcessingService orderAsyncProcessingService;

    public LocalOrderEventPublisher(OrderAsyncProcessingService orderAsyncProcessingService) {
        this.orderAsyncProcessingService = orderAsyncProcessingService;
    }

    @Override
    public void publishOrderCreated(Long orderId) {
        if (orderId == null) {
            return;
        }
        orderAsyncProcessingService.processOrderCreated(orderId);
    }
}
