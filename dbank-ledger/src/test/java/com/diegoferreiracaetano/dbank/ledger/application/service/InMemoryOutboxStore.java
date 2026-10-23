package com.diegoferreiracaetano.dbank.ledger.application.service;

import com.diegoferreiracaetano.dbank.ledger.application.outbox.OutboxMessage;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.OutboxStore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class InMemoryOutboxStore implements OutboxStore {

    private final List<OutboxMessage> pending = new ArrayList<>();
    private final List<UUID> sent = new ArrayList<>();
    private Instant sentAt;

    void add(OutboxMessage... messages) {
        pending.addAll(List.of(messages));
    }

    @Override
    public List<OutboxMessage> lockPending(int limit) {
        return pending.stream().limit(limit).toList();
    }

    @Override
    public void markSent(List<UUID> ids, Instant at) {
        sent.addAll(ids);
        pending.removeIf(message -> ids.contains(message.id()));
        sentAt = at;
    }

    List<OutboxMessage> pending() {
        return List.copyOf(pending);
    }

    List<UUID> sent() {
        return List.copyOf(sent);
    }

    Instant sentAt() {
        return sentAt;
    }
}
