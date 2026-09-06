package com.challenge.order.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(Long orderId) {
        super("No se encontró la orden " + orderId);
    }
}
