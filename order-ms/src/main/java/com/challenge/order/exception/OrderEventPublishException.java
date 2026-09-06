package com.challenge.order.exception;

public class OrderEventPublishException extends RuntimeException {

    public OrderEventPublishException(Throwable cause) {
        super("No fue posible registrar la orden en este momento", cause);
    }
}
