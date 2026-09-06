package com.challenge.payment.messaging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PaymentKafkaConfigurationTest {

    @Test
    void exhaustsExactlyThreeAttemptsAndLogsOnlySafeMetadata() {
        Logger logger = (Logger) LoggerFactory.getLogger(PaymentKafkaConfiguration.class);
        ListAppender<ILoggingEvent> logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
        DefaultErrorHandler handler = new PaymentKafkaConfiguration().paymentErrorHandler();
        MessageListenerContainer container = mock(MessageListenerContainer.class);
        when(container.isRunning()).thenReturn(true);
        ConsumerRecord<String, OrderPlacedEvent> record = new ConsumerRecord<>("order-placed", 0, 4L,
                "7", new OrderPlacedEvent(7L, "sensitive-cipher"));
        RuntimeException failure = new IllegalArgumentException("sensitive-cause");
        try {
            assertThat(handler.handleOne(failure, record, null, container)).isFalse();
            assertThat(handler.handleOne(failure, record, null, container)).isFalse();
            assertThat(handler.handleOne(failure, record, null, container)).isTrue();
            assertThat(logs.list).hasSize(4);
            assertThat(logs.list.get(3).getFormattedMessage()).contains("agotó 3 intentos", "orderId=7");
            assertThat(logs.list).allSatisfy(log -> {
                assertThat(log.getFormattedMessage()).doesNotContain("sensitive-cipher", "sensitive-cause");
                assertThat(log.getThrowableProxy()).isNull();
            });
        } finally {
            handler.clearThreadState();
            logger.detachAppender(logs);
            logs.stop();
        }
    }
}
