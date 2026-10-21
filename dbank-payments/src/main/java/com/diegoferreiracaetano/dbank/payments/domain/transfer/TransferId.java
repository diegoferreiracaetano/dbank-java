package com.diegoferreiracaetano.dbank.payments.domain.transfer;

import java.util.UUID;

public record TransferId(UUID value) {

    public TransferId {
        if (value == null) {
            throw new IllegalArgumentException("TransferId value is required");
        }
    }

    public static TransferId generate() {
        return new TransferId(UUID.randomUUID());
    }
}
