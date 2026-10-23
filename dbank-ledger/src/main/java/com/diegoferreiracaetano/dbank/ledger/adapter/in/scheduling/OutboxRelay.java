package com.diegoferreiracaetano.dbank.ledger.adapter.in.scheduling;

import com.diegoferreiracaetano.dbank.ledger.application.port.in.PublishPendingEventsUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class OutboxRelay {

    private final PublishPendingEventsUseCase useCase;

    OutboxRelay(PublishPendingEventsUseCase useCase) {
        this.useCase = useCase;
    }

    @Scheduled(fixedDelayString = "${dbank.outbox.poll-interval}")
    void relay() {
        useCase.publishPending();
    }
}
