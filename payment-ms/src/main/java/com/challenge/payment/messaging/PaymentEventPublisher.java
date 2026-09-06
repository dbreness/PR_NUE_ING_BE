package com.challenge.payment.messaging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class PaymentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public PaymentEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topics.payment-processed}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(PaymentProcessedEvent event) {
        try {
            // Esperar confirmación permite reintentar también los fallos asíncronos.
            kafkaTemplate.send(topic, event.orderId().toString(), event).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PaymentEventProcessingException();
        } catch (ExecutionException | RuntimeException exception) {
            throw new PaymentEventProcessingException();
        }
    }
}
