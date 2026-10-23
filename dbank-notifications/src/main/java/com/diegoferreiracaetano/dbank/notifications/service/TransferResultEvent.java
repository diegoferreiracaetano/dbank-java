package com.diegoferreiracaetano.dbank.notifications.service;

import java.util.UUID;

public record TransferResultEvent(UUID eventId, Outcome outcome, UUID transferId, UUID accountId, String reason) {

    public enum Outcome {
        SETTLED,
        REJECTED
    }

    public TransferResultEvent {
        if (eventId == null || outcome == null || transferId == null || accountId == null) {
            throw new IllegalArgumentException("eventId, outcome, transferId and accountId are required");
        }
    }
}
