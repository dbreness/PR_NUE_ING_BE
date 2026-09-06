package com.challenge.payment.service;

public class PaymentProcessingException extends RuntimeException {

    public PaymentProcessingException(Throwable cause) {
        super("El procesamiento del pago fue interrumpido", cause);
    }
}
