package com.challenge.order.messaging;

public record OrderPlacedEvent(Long orderId, String encryptedCardData) {
}
