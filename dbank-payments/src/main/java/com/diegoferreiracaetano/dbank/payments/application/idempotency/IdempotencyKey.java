package com.diegoferreiracaetano.dbank.payments.application.idempotency;

public record IdempotencyKey(String value) {

    private static final int MAX_LENGTH = 255;

    public IdempotencyKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Idempotency key is required");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Idempotency key must have at most " + MAX_LENGTH + " characters");
        }
    }
}
