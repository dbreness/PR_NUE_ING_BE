package com.challenge.payment.messaging;

public record OrderPlacedEvent(Long orderId, String encryptedCardData) {

    @Override
    public String toString() {
        return "OrderPlacedEvent[orderId=" + orderId + ", encryptedCardData=[REDACTED]]";
    }
}
