package com.diegoferreiracaetano.dbank.ledger.adapter.out.persistence.outbox;

import com.diegoferreiracaetano.dbank.ledger.application.port.out.TransferResultEvents;
import com.diegoferreiracaetano.dbank.ledger.application.transfer.TransferOutcome;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransactionId;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class OutboxTransferResultEvents implements TransferResultEvents {

    static final String TOPIC = "ledger.transfer-result.v1";

    private final SpringDataOutboxRepository repository;
    private final ObjectMapper mapper;
    private final Clock clock;

    OutboxTransferResultEvents(SpringDataOutboxRepository repository, ObjectMapper mapper, Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    public void transferProcessed(LedgerTransactionId transferId, AccountId from, TransferOutcome outcome) {
        var now = clock.instant();
        var envelope = switch (outcome) {
            case TransferOutcome.Settled() ->
                new Envelope(
                        UUID.randomUUID(), "TransferSettled", now, new Payload(transferId.value(), from.value(), null));
            case TransferOutcome.Rejected(var reason) ->
                new Envelope(
                        UUID.randomUUID(),
                        "TransferRejected",
                        now,
                        new Payload(transferId.value(), from.value(), reason.name()));
        };
        repository.save(
                new OutboxEventEntity(envelope.eventId(), TOPIC, from.value().toString(), write(envelope), now));
    }

    private String write(Envelope envelope) {
        try {
            return mapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize event " + envelope.eventId(), e);
        }
    }

    private record Envelope(UUID eventId, String eventType, Instant occurredAt, Payload payload) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record Payload(UUID transferId, UUID fromAccountId, String reason) {}
}
