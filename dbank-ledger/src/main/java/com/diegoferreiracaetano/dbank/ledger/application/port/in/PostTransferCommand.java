package com.diegoferreiracaetano.dbank.ledger.application.port.in;

import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransactionId;
import com.diegoferreiracaetano.dbank.ledger.domain.shared.Money;
import java.util.Objects;

public record PostTransferCommand(LedgerTransactionId transactionId, AccountId from, AccountId to, Money amount) {

    public PostTransferCommand {
        Objects.requireNonNull(transactionId, "transactionId is required");
        Objects.requireNonNull(from, "from is required");
        Objects.requireNonNull(to, "to is required");
        Objects.requireNonNull(amount, "amount is required");
    }
}
