package com.challenge.order.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentProcessedEventDeserializationTest {

    @Test
    void deserializesContractWithoutJavaTypeHeaders() {
        JsonDeserializer<PaymentProcessedEvent> deserializer = new JsonDeserializer<>();
        deserializer.configure(Map.of(
                JsonDeserializer.VALUE_DEFAULT_TYPE, PaymentProcessedEvent.class.getName(),
                JsonDeserializer.USE_TYPE_INFO_HEADERS, false,
                JsonDeserializer.TRUSTED_PACKAGES, "com.challenge.order.messaging"
        ), false);

        PaymentProcessedEvent event = deserializer.deserialize(
                "payment-processed",
                "{\"orderId\":7,\"result\":\"SUCCESS\"}".getBytes(StandardCharsets.UTF_8)
        );

        assertThat(event.orderId()).isEqualTo(7L);
        assertThat(event.result()).isEqualTo(PaymentProcessedEvent.Result.SUCCESS);
        deserializer.close();
    }
}
