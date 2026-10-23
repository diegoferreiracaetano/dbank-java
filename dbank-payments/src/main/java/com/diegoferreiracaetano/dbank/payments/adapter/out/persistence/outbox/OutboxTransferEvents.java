package com.diegoferreiracaetano.dbank.payments.adapter.out.persistence.outbox;

import com.diegoferreiracaetano.dbank.payments.application.port.out.TransferEvents;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.Transfer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class OutboxTransferEvents implements TransferEvents {

    static final String TOPIC = "payments.transfer-requested.v1";

    private final SpringDataOutboxRepository repository;
    private final ObjectMapper mapper;
    private final Clock clock;

    OutboxTransferEvents(SpringDataOutboxRepository repository, ObjectMapper mapper, Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    public void transferRequested(Transfer transfer) {
        var now = clock.instant();
        var payload = new Payload(
                transfer.id().value(),
                transfer.from().value(),
                transfer.to().value(),
                transfer.amount().cents());
        var envelope = new Envelope(UUID.randomUUID(), "TransferRequested", now, payload);
        repository.save(new OutboxEventEntity(
                envelope.eventId(), TOPIC, transfer.from().value().toString(), write(envelope), now));
    }

    private String write(Envelope envelope) {
        try {
            return mapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize event " + envelope.eventId(), e);
        }
    }

    private record Envelope(UUID eventId, String eventType, Instant occurredAt, Payload payload) {}

    private record Payload(UUID transferId, UUID fromAccountId, UUID toAccountId, long amountCents) {}
}
