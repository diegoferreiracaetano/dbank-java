package com.diegoferreiracaetano.dbank.ledger.application.port.out;

import com.diegoferreiracaetano.dbank.ledger.application.outbox.OutboxMessage;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxStore {

    List<OutboxMessage> lockPending(int limit);

    void markSent(List<UUID> ids, Instant sentAt);
}
