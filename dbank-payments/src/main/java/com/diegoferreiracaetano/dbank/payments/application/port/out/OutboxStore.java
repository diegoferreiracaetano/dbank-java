package com.diegoferreiracaetano.dbank.payments.application.port.out;

import com.diegoferreiracaetano.dbank.payments.application.outbox.OutboxMessage;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxStore {

    List<OutboxMessage> lockPending(int limit);

    void markSent(List<UUID> ids, Instant sentAt);
}
