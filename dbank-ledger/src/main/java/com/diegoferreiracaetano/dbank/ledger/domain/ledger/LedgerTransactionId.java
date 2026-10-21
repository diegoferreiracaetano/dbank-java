package com.diegoferreiracaetano.dbank.ledger.domain.ledger;

import java.util.UUID;

public record LedgerTransactionId(UUID value) {

    public LedgerTransactionId {
        if (value == null) {
            throw new IllegalArgumentException("LedgerTransactionId value is required");
        }
    }

    public static LedgerTransactionId generate() {
        return new LedgerTransactionId(UUID.randomUUID());
    }
}
