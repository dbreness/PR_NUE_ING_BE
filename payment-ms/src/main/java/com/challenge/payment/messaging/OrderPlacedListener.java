package com.challenge.payment.messaging;

import com.challenge.payment.security.CardData;
import com.challenge.payment.security.RsaCardDataDecryptor;
import com.challenge.payment.service.PaymentProcessor;
import com.challenge.payment.service.PaymentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderPlacedListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderPlacedListener.class);

    private final RsaCardDataDecryptor decryptor;
    private final PaymentProcessor processor;
    private final PaymentEventPublisher publisher;

    public OrderPlacedListener(
            RsaCardDataDecryptor decryptor,
            PaymentProcessor processor,
            PaymentEventPublisher publisher
    ) {
        this.decryptor = decryptor;
        this.processor = processor;
        this.publisher = publisher;
    }

    @KafkaListener(topics = "${app.kafka.topics.order-placed}")
    public void onOrderPlaced(OrderPlacedEvent event) {
        try {
            if (event == null || event.orderId() == null || event.orderId() <= 0
                    || event.encryptedCardData() == null || event.encryptedCardData().isBlank()) {
                throw new PaymentEventProcessingException();
            }
            LOGGER.info("Recibido order-placed: orderId={}", event.orderId());
            CardData cardData = decryptor.decrypt(event.encryptedCardData());
            PaymentResult result = processor.process(cardData);
            PaymentProcessedEvent.Result eventResult = switch (result) {
                case SUCCESS -> PaymentProcessedEvent.Result.SUCCESS;
                case FAILED -> PaymentProcessedEvent.Result.FAILED;
            };
            publisher.publish(new PaymentProcessedEvent(event.orderId(), eventResult));
            LOGGER.info("Publicado payment-processed: orderId={}, result={}", event.orderId(), eventResult);
        } catch (RuntimeException exception) {
            // Kafka puede registrar la excepción; no propagar sus causas sensibles.
            throw new PaymentEventProcessingException();
        }
    }
}
