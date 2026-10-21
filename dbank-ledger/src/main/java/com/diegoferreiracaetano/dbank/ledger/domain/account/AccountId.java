package com.diegoferreiracaetano.dbank.ledger.domain.account;

import java.util.UUID;

public record AccountId(UUID value) {

    public AccountId {
        if (value == null) {
            throw new IllegalArgumentException("AccountId value is required");
        }
    }

    public static AccountId generate() {
        return new AccountId(UUID.randomUUID());
    }
}
