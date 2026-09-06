package com.challenge.payment.messaging;

public record PaymentProcessedEvent(Long orderId, Result result) {

    public enum Result {
        SUCCESS,
        FAILED
    }
}
