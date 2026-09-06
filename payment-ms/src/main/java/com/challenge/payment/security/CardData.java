package com.challenge.payment.security;

public record CardData(String cardNumber, String expiration, String cvv) {

    public CardData {
        if (isBlank(cardNumber) || isBlank(expiration) || isBlank(cvv)) {
            throw new IllegalArgumentException("Los datos de tarjeta están incompletos");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @Override
    public String toString() {
        return "CardData[REDACTED]";
    }
}
