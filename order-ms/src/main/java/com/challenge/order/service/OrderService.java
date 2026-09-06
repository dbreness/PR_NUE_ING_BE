package com.challenge.order.service;

import com.challenge.order.dto.CreateOrderRequest;
import com.challenge.order.dto.OrderPageResponse;
import com.challenge.order.dto.OrderResponse;
import com.challenge.order.exception.OrderNotFoundException;
import com.challenge.order.messaging.OrderEventPublisher;
import com.challenge.order.messaging.OrderPlacedEvent;
import com.challenge.order.model.Order;
import com.challenge.order.model.OrderStatus;
import com.challenge.order.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository, OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        Order order = new Order(
                request.productName().trim(),
                request.quantity(),
                request.amount(),
                request.encryptedCardData()
        );
        Order savedOrder = orderRepository.save(order);
        eventPublisher.publishOrderPlaced(new OrderPlacedEvent(savedOrder.getId(), savedOrder.getEncryptedCardData()));
        return OrderResponse.from(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long orderId) {
        return orderRepository.findById(orderId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional(readOnly = true)
    public OrderPageResponse findAll(OrderStatus status, String productName, Pageable pageable) {
        Page<OrderResponse> page = orderRepository
                .findAll(OrderSpecifications.withFilters(status, productName), pageable)
                .map(OrderResponse::from);
        return OrderPageResponse.from(page);
    }
}
