package com.clothing.messaging.publisher;

public interface OrderEventPublisher {

    void publishOrderCreated(Long orderId);
}
