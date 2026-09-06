package com.challenge.order.service;

import com.challenge.order.dto.CreateOrderRequest;
import com.challenge.order.dto.OrderResponse;
import com.challenge.order.exception.OrderEventPublishException;
import com.challenge.order.messaging.OrderEventPublisher;
import com.challenge.order.model.Order;
import com.challenge.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createPublishesEventAndReturnsPublicResponse() {
        CreateOrderRequest request = new CreateOrderRequest("  Teclado  ", 2, new BigDecimal("49.90"), "cipher");
        Order savedOrder = new Order("Teclado", 2, new BigDecimal("49.90"), "cipher");
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderResponse response = orderService.create(request);

        assertThat(response.productName()).isEqualTo("Teclado");
        assertThat(response.status()).hasToString("PENDIENTE");
        verify(eventPublisher).publishOrderPlaced(any());
    }

    @Test
    void createPropagatesPublishFailureForTransactionRollback() {
        CreateOrderRequest request = new CreateOrderRequest("Teclado", 1, BigDecimal.TEN, "cipher");
        Order savedOrder = new Order("Teclado", 1, BigDecimal.TEN, "cipher");
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        doThrow(new OrderEventPublishException(new IllegalStateException("Kafka no disponible")))
                .when(eventPublisher).publishOrderPlaced(any());

        org.junit.jupiter.api.Assertions.assertThrows(
                OrderEventPublishException.class,
                () -> orderService.create(request)
        );
        verify(orderRepository).save(any(Order.class));
    }
}
