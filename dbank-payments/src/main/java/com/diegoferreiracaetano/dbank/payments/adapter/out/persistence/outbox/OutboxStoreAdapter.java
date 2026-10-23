package com.diegoferreiracaetano.dbank.payments.adapter.out.persistence.outbox;

import com.diegoferreiracaetano.dbank.payments.application.outbox.OutboxMessage;
import com.diegoferreiracaetano.dbank.payments.application.port.out.OutboxStore;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class OutboxStoreAdapter implements OutboxStore {

    private final SpringDataOutboxRepository repository;

    OutboxStoreAdapter(SpringDataOutboxRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<OutboxMessage> lockPending(int limit) {
        return repository.lockPending(limit).stream()
                .map(entity -> new OutboxMessage(
                        entity.getId(),
                        entity.getTopic(),
                        entity.getMessageKey(),
                        entity.getPayload(),
                        entity.getCreatedAt()))
                .toList();
    }

    @Override
    public void markSent(List<UUID> ids, Instant sentAt) {
        repository.markSent(ids, sentAt);
    }
}
