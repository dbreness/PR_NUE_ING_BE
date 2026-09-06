package com.challenge.payment.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PaymentEventPublisherTest {

    @Test
    @SuppressWarnings("unchecked")
    void publishesUsingOrderIdAsKeyAndWaitsForConfirmation() throws Exception {
        KafkaTemplate<String, Object> template = mock(KafkaTemplate.class);
        PaymentProcessedEvent event = new PaymentProcessedEvent(7L, PaymentProcessedEvent.Result.SUCCESS);
        CompletableFuture<SendResult<String, Object>> confirmation = mock(CompletableFuture.class);
        when(template.send("payment-processed", "7", event)).thenReturn(confirmation);

        new PaymentEventPublisher(template, "payment-processed").publish(event);

        verify(confirmation).get();
    }

    @Test
    @SuppressWarnings("unchecked")
    void propagatesFailedAcknowledgementAsSanitizedTechnicalFailure() {
        KafkaTemplate<String, Object> template = mock(KafkaTemplate.class);
        PaymentProcessedEvent event = new PaymentProcessedEvent(7L, PaymentProcessedEvent.Result.FAILED);
        when(template.send("payment-processed", "7", event))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("internal-detail")));

        assertThatThrownBy(() -> new PaymentEventPublisher(template, "payment-processed").publish(event))
                .isInstanceOf(PaymentEventProcessingException.class).hasNoCause();
    }
}
