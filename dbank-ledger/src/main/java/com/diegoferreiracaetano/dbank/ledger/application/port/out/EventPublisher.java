package com.diegoferreiracaetano.dbank.ledger.application.port.out;

import com.diegoferreiracaetano.dbank.ledger.application.outbox.OutboxMessage;

public interface EventPublisher {

    void publish(OutboxMessage message);
}
