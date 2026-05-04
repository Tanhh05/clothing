package com.clothing.messaging.publisher;

import com.clothing.messaging.event.OrderCreatedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(prefix = "app.integrations.rabbit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitOrderEventPublisher implements OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public RabbitOrderEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${app.messaging.order.exchange:order.exchange}") String exchange,
            @Value("${app.messaging.order.routing-key:order.created}") String routingKey
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    @Override
    public void publishOrderCreated(Long orderId) {
        OrderCreatedEvent event = new OrderCreatedEvent(orderId, LocalDateTime.now());
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }
}
