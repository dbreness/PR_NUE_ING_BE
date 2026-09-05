package com.challenge.order.dto;

import com.challenge.order.model.Order;
import com.challenge.order.model.OrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OrderResponse(
        Long id,
        String productName,
        Integer quantity,
        BigDecimal amount,
        OrderStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getProductName(),
                order.getQuantity(),
                order.getAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
