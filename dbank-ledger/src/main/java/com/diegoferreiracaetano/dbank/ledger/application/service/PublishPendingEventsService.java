package com.diegoferreiracaetano.dbank.ledger.application.service;

import com.diegoferreiracaetano.dbank.ledger.application.outbox.EventPublishException;
import com.diegoferreiracaetano.dbank.ledger.application.port.in.PublishPendingEventsUseCase;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.EventPublisher;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.OutboxStore;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.TransactionRunner;
import java.time.Clock;
import java.util.ArrayList;
import java.util.UUID;

public class PublishPendingEventsService implements PublishPendingEventsUseCase {

    private final OutboxStore outbox;
    private final EventPublisher publisher;
    private final TransactionRunner transactionRunner;
    private final Clock clock;
    private final int batchSize;

    public PublishPendingEventsService(
            OutboxStore outbox,
            EventPublisher publisher,
            TransactionRunner transactionRunner,
            Clock clock,
            int batchSize) {
        this.outbox = outbox;
        this.publisher = publisher;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
        this.batchSize = batchSize;
    }

    @Override
    public int publishPending() {
        return transactionRunner.call(this::publishBatch);
    }

    private int publishBatch() {
        var sent = new ArrayList<UUID>();
        for (var message : outbox.lockPending(batchSize)) {
            try {
                publisher.publish(message);
            } catch (EventPublishException e) {
                break;
            }
            sent.add(message.id());
        }
        if (!sent.isEmpty()) {
            outbox.markSent(sent, clock.instant());
        }
        return sent.size();
    }
}
