package com.diegoferreiracaetano.dbank.payments.application.outbox;

import java.util.UUID;

public class EventPublishException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EventPublishException(UUID eventId, Throwable cause) {
        super("Could not publish event " + eventId, cause);
    }
}
