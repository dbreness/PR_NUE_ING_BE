package com.challenge.order.messaging;

import com.challenge.order.exception.OrderEventPublishException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String orderPlacedTopic;

    public OrderEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topics.order-placed:order-placed}") String orderPlacedTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.orderPlacedTopic = orderPlacedTopic;
    }

    public void publishOrderPlaced(OrderPlacedEvent event) {
        try {
            kafkaTemplate.send(orderPlacedTopic, String.valueOf(event.orderId()), event).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new OrderEventPublishException(exception);
        } catch (ExecutionException exception) {
            throw new OrderEventPublishException(exception.getCause());
        }
    }
}
