package com.challenge.payment.messaging;

import com.challenge.payment.security.CardData;
import com.challenge.payment.security.RsaCardDataDecryptor;
import com.challenge.payment.service.PaymentProcessor;
import com.challenge.payment.service.PaymentResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderPlacedListenerTest {

    @Mock
    private RsaCardDataDecryptor decryptor;

    @Mock
    private PaymentProcessor processor;

    @Mock
    private PaymentEventPublisher publisher;

    @ParameterizedTest
    @EnumSource(PaymentResult.class)
    void publishesBothBusinessOutcomesWithoutRetry(PaymentResult result) {
        CardData card = new CardData("test-number", "test-expiration", "test-code");
        when(decryptor.decrypt("cipher")).thenReturn(card);
        when(processor.process(card)).thenReturn(result);

        new OrderPlacedListener(decryptor, processor, publisher)
                .onOrderPlaced(new OrderPlacedEvent(7L, "cipher"));

        verify(publisher).publish(new PaymentProcessedEvent(7L,
                PaymentProcessedEvent.Result.valueOf(result.name())));
        verify(processor, times(1)).process(card);
    }

    @Test
    void decryptionFailureDoesNotPublishAndDoesNotExposeItsCause() {
        when(decryptor.decrypt("cipher")).thenThrow(new IllegalArgumentException("sensitive-json"));
        assertThatThrownBy(() -> new OrderPlacedListener(decryptor, processor, publisher)
                .onOrderPlaced(new OrderPlacedEvent(7L, "cipher")))
                .isInstanceOf(PaymentEventProcessingException.class)
                .hasMessage("No fue posible procesar el evento de pago")
                .hasNoCause();
        verifyNoInteractions(processor, publisher);
    }

    @Test
    void rejectsIncompleteEventBeforeDecryption() {
        assertThatThrownBy(() -> new OrderPlacedListener(decryptor, processor, publisher)
                .onOrderPlaced(new OrderPlacedEvent(null, "cipher")))
                .isInstanceOf(PaymentEventProcessingException.class);
        verifyNoInteractions(decryptor, processor, publisher);
    }
}
