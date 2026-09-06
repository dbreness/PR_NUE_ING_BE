package com.challenge.payment.service;

import com.challenge.payment.security.CardData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class SimulatedPaymentProcessor implements PaymentProcessor {

    private final double successRate;
    private final long processingDelayMs;

    public SimulatedPaymentProcessor(
            @Value("${app.payment.success-rate:0.85}") double successRate,
            @Value("${app.payment.processing-delay-ms:2000}") long processingDelayMs
    ) {
        if (successRate < 0 || successRate > 1) {
            throw new IllegalArgumentException("La probabilidad de éxito debe estar entre 0 y 1");
        }
        if (processingDelayMs < 0) {
            throw new IllegalArgumentException("La demora de procesamiento no puede ser negativa");
        }
        this.successRate = successRate;
        this.processingDelayMs = processingDelayMs;
    }

    @Override
    public PaymentResult process(CardData cardData) {
        waitForProcessing();
        return ThreadLocalRandom.current().nextDouble() < successRate
                ? PaymentResult.SUCCESS
                : PaymentResult.FAILED;
    }

    private void waitForProcessing() {
        try {
            Thread.sleep(processingDelayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PaymentProcessingException(exception);
        }
    }
}
