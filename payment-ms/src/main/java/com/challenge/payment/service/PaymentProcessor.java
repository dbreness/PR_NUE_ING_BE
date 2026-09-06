package com.challenge.payment.service;

import com.challenge.payment.security.CardData;

public interface PaymentProcessor {

    PaymentResult process(CardData cardData);
}
