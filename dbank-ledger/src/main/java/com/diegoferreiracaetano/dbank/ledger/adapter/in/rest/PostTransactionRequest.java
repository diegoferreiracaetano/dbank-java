package com.diegoferreiracaetano.dbank.ledger.adapter.in.rest;

import com.diegoferreiracaetano.dbank.ledger.application.port.in.PostTransferCommand;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransactionId;
import com.diegoferreiracaetano.dbank.ledger.domain.shared.Money;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

record PostTransactionRequest(
        @Schema(
                description = "Client-generated id. Reposting the same id is a no-op.",
                example = "6f1c0a52-3d3e-4c1f-9d4b-2a7f6e0b9a11")
        @NotNull
        UUID transactionId,

        @Schema(
                description = "Account to debit. In the dev profile this id is a funded treasury.",
                example = "00000000-0000-0000-0000-000000000001")
        @NotNull
        UUID fromAccountId,

        @Schema(description = "Account to credit. Use the id returned by POST /v1/accounts.") @NotNull
        UUID toAccountId,

        @Schema(description = "Amount in cents; must be positive.", example = "1000") @Positive
        long amountCents) {

    PostTransferCommand toCommand() {
        return new PostTransferCommand(
                new LedgerTransactionId(transactionId),
                new AccountId(fromAccountId),
                new AccountId(toAccountId),
                Money.ofCents(amountCents));
    }
}
