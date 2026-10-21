package com.diegoferreiracaetano.dbank.ledger.adapter.in.rest;

import com.diegoferreiracaetano.dbank.ledger.application.port.in.PostTransferCommand;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransactionId;
import com.diegoferreiracaetano.dbank.ledger.domain.shared.Money;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

record PostTransactionRequest(
        @NotNull UUID transactionId,
        @NotNull UUID fromAccountId,
        @NotNull UUID toAccountId,
        @Positive long amountCents) {

    PostTransferCommand toCommand() {
        return new PostTransferCommand(
                new LedgerTransactionId(transactionId),
                new AccountId(fromAccountId),
                new AccountId(toAccountId),
                Money.ofCents(amountCents));
    }
}
