package com.challenge.order.messaging;

import com.challenge.order.model.Order;
import com.challenge.order.model.OrderStatus;
import com.challenge.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentProcessedListenerTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PaymentProcessedListener listener;

    @Test
    void successChangesPendingOrderToPaid() {
        Order order = new Order("Teclado", 1, BigDecimal.TEN, "cipher");
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));

        listener.onPaymentProcessed(new PaymentProcessedEvent(7L, PaymentProcessedEvent.Result.SUCCESS));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAGADO);
        verify(orderRepository).findById(7L);
    }

    @Test
    void duplicateEventDoesNotChangeTerminalOrder() {
        Order order = new Order("Teclado", 1, BigDecimal.TEN, "cipher");
        order.updateStatus(OrderStatus.PAGADO);
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));

        listener.onPaymentProcessed(new PaymentProcessedEvent(7L, PaymentProcessedEvent.Result.FAILED));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAGADO);
    }
}
