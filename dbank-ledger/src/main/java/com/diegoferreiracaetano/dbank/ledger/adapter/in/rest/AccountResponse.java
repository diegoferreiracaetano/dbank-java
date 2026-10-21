package com.diegoferreiracaetano.dbank.ledger.adapter.in.rest;

import com.diegoferreiracaetano.dbank.ledger.domain.account.Account;
import java.util.UUID;

record AccountResponse(UUID id, long balanceCents) {

    static AccountResponse from(Account account) {
        return new AccountResponse(account.id().value(), account.balance().cents());
    }
}
