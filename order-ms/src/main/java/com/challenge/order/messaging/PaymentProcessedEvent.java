package com.challenge.order.messaging;

public record PaymentProcessedEvent(Long orderId, Result result) {

    public enum Result {
        SUCCESS,
        FAILED
    }
}
