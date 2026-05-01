package com.clothing.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "app.integrations.rabbit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitConfig {

    @Bean
    public Queue orderProcessingQueue(
            @Value("${app.messaging.order.queue:order.processing.queue}") String queueName
    ) {
        return new Queue(queueName, true);
    }

    @Bean
    public TopicExchange orderExchange(
            @Value("${app.messaging.order.exchange:order.exchange}") String exchangeName
    ) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Binding orderProcessingBinding(
            Queue orderProcessingQueue,
            TopicExchange orderExchange,
            @Value("${app.messaging.order.routing-key:order.created}") String routingKey
    ) {
        return BindingBuilder.bind(orderProcessingQueue).to(orderExchange).with(routingKey);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
