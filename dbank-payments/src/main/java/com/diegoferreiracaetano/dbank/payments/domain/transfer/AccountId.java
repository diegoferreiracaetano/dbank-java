package com.diegoferreiracaetano.dbank.payments.domain.transfer;

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
