package com.challenge.order.messaging;

import com.challenge.order.model.OrderStatus;
import com.challenge.order.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentProcessedListener {

    private final OrderRepository orderRepository;

    public PaymentProcessedListener(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    @KafkaListener(
            topics = "${app.kafka.topics.payment-processed:payment-processed}",
            groupId = "${spring.application.name}"
    )
    public void onPaymentProcessed(PaymentProcessedEvent event) {
        orderRepository.findById(event.orderId()).ifPresent(order -> {
            if (!order.isPending()) {
                return;
            }
            OrderStatus newStatus = event.result() == PaymentProcessedEvent.Result.SUCCESS
                    ? OrderStatus.PAGADO
                    : OrderStatus.FALLO_PAGO;
            order.updateStatus(newStatus);
        });
    }
}
