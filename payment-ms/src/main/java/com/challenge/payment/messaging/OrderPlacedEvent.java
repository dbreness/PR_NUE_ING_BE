package com.challenge.payment.messaging;

public record OrderPlacedEvent(Long orderId, String encryptedCardData) {
}
