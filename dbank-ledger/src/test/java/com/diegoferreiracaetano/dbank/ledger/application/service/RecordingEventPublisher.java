package com.diegoferreiracaetano.dbank.ledger.application.service;

import com.diegoferreiracaetano.dbank.ledger.application.outbox.EventPublishException;
import com.diegoferreiracaetano.dbank.ledger.application.outbox.OutboxMessage;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.EventPublisher;
import java.util.ArrayList;
import java.util.List;

class RecordingEventPublisher implements EventPublisher {

    private final List<OutboxMessage> published = new ArrayList<>();
    private String failingKey;

    void failOn(String key) {
        this.failingKey = key;
    }

    @Override
    public void publish(OutboxMessage message) {
        if (message.key().equals(failingKey)) {
            throw new EventPublishException(message.id(), new IllegalStateException("broker down"));
        }
        published.add(message);
    }

    List<OutboxMessage> published() {
        return List.copyOf(published);
    }
}
