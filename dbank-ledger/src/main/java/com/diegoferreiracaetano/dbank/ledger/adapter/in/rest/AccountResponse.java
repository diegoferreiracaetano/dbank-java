package com.diegoferreiracaetano.dbank.ledger.adapter.in.rest;

import com.diegoferreiracaetano.dbank.ledger.domain.account.Account;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

record AccountResponse(
        @Schema(description = "Account id", example = "6f1c0a52-3d3e-4c1f-9d4b-2a7f6e0b9a11")
        UUID id,

        @Schema(description = "Current balance in cents", example = "1000")
        long balanceCents) {

    static AccountResponse from(Account account) {
        return new AccountResponse(account.id().value(), account.balance().cents());
    }
}
