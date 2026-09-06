package com.challenge.payment;

import com.challenge.payment.messaging.OrderPlacedListener;
import com.challenge.payment.messaging.OrderPlacedEvent;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentApplicationTest {

    @TempDir
    private Path directory;

    @Test
    void startsConsumerContextWithLocalPrivateKeyAndNoWebOrDatabase() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        Path key = directory.resolve("private-key.pem");
        Files.writeString(key, "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getEncoder().encodeToString(generator.generateKeyPair().getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n");
        new ApplicationContextRunner().withUserConfiguration(PaymentApplication.class)
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("app.rsa.private-key-path=" + key,
                        "spring.kafka.listener.auto-startup=false")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(OrderPlacedListener.class);
                    try (ErrorHandlingDeserializer<OrderPlacedEvent> deserializer = new ErrorHandlingDeserializer<>()) {
                        deserializer.configure(context.getBean(KafkaProperties.class).buildConsumerProperties(null), false);
                        RecordHeaders headers = new RecordHeaders();
                        headers.add("__TypeId__", "com.challenge.order.messaging.OrderPlacedEvent"
                                .getBytes(StandardCharsets.UTF_8));
                        OrderPlacedEvent event = deserializer.deserialize("order-placed", headers,
                                "{\"orderId\":7,\"encryptedCardData\":\"cipher\"}".getBytes(StandardCharsets.UTF_8));
                        assertThat(event).isEqualTo(new OrderPlacedEvent(7L, "cipher"));
                        RecordHeaders invalidHeaders = new RecordHeaders();
                        assertThat(deserializer.deserialize("order-placed", invalidHeaders,
                                "invalid-json".getBytes(StandardCharsets.UTF_8))).isNull();
                        assertThat(invalidHeaders.lastHeader("springDeserializerExceptionValue")).isNotNull();
                    }
                });
    }
}
