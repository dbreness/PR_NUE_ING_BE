package com.challenge.payment.service;

import com.challenge.payment.security.CardData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimulatedPaymentProcessorTest {

    private static final CardData CARD_DATA = new CardData("test-number", "12/30", "test-code");

    @Test
    void successRateOneAlwaysSucceeds() {
        PaymentResult result = new SimulatedPaymentProcessor(1.0, 0).process(CARD_DATA);

        assertThat(result).isEqualTo(PaymentResult.SUCCESS);
    }

    @Test
    void successRateZeroAlwaysFails() {
        PaymentResult result = new SimulatedPaymentProcessor(0.0, 0).process(CARD_DATA);

        assertThat(result).isEqualTo(PaymentResult.FAILED);
    }

    @ParameterizedTest
    @CsvSource({"-0.01, 0", "1.01, 0", "0.85, -1"})
    void rejectsInvalidConfiguration(double successRate, long delayMs) {
        assertThatThrownBy(() -> new SimulatedPaymentProcessor(successRate, delayMs))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void preservesInterruptFlagWhenDelayIsInterrupted() {
        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(() -> new SimulatedPaymentProcessor(1.0, 2_000).process(CARD_DATA))
                    .isInstanceOf(PaymentProcessingException.class)
                    .hasMessage("El procesamiento del pago fue interrumpido");
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }
}
