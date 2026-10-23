package com.diegoferreiracaetano.dbank.payments.application.port.out;

import com.diegoferreiracaetano.dbank.payments.application.outbox.OutboxMessage;

public interface EventPublisher {

    void publish(OutboxMessage message);
}
