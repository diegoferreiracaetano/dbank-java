package com.diegoferreiracaetano.dbank.payments.adapter.out.messaging;

import com.diegoferreiracaetano.dbank.payments.application.outbox.EventPublishException;
import com.diegoferreiracaetano.dbank.payments.application.outbox.OutboxMessage;
import com.diegoferreiracaetano.dbank.payments.application.port.out.EventPublisher;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
class KafkaEventPublisher implements EventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, String> kafka;

    KafkaEventPublisher(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    @Override
    public void publish(OutboxMessage message) {
        try {
            kafka.send(message.topic(), message.key(), message.payload()).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EventPublishException(message.id(), e);
        } catch (ExecutionException | TimeoutException e) {
            LOG.warn("Could not publish event {}; it stays pending", message.id(), e);
            throw new EventPublishException(message.id(), e);
        }
    }
}
