package com.challenge.payment.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

@Configuration
public class PaymentKafkaConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentKafkaConfiguration.class);

    @Bean
    public DefaultErrorHandler paymentErrorHandler() {
        DefaultErrorHandler handler = new DefaultErrorHandler((record, exception) -> {
            Long orderId = record.value() instanceof OrderPlacedEvent event ? event.orderId() : null;
            LOGGER.error("Evento de pago agotó 3 intentos; sin publicar resultado: orderId={}, partition={}, offset={}",
                    orderId, record.partition(), record.offset());
        }, new FixedBackOff(1_000L, 2L));
        // Un intento inicial y dos reintentos, incluso para errores de deserialización.
        handler.setClassifications(Map.of(), true);
        handler.setResetStateOnExceptionChange(false);
        handler.setSeekAfterError(false);
        handler.setRetryListeners((record, exception, attempt) ->
                LOGGER.warn("Falló procesamiento de pago: intento={}, partition={}, offset={}",
                        attempt, record.partition(), record.offset()));
        return handler;
    }
}
