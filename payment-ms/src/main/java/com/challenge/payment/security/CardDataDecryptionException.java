package com.challenge.payment.security;

public class CardDataDecryptionException extends RuntimeException {

    public CardDataDecryptionException(Throwable cause) {
        super("No fue posible descifrar los datos de pago", cause);
    }
}
