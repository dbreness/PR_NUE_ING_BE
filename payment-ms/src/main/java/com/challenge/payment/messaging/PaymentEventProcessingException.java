package com.challenge.payment.messaging;

public class PaymentEventProcessingException extends RuntimeException {

    public PaymentEventProcessingException() {
        // No conservar causas que puedan contener el JSON descifrado.
        super("No fue posible procesar el evento de pago");
    }
}
