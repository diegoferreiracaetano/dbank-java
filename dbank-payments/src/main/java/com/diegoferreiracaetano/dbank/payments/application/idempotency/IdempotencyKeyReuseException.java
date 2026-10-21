package com.diegoferreiracaetano.dbank.payments.application.idempotency;

public class IdempotencyKeyReuseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public IdempotencyKeyReuseException(IdempotencyKey key) {
        super("Idempotency key already used with a different request: " + key.value());
    }
}
