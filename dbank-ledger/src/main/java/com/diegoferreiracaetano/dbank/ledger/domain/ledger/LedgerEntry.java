package com.diegoferreiracaetano.dbank.ledger.domain.ledger;

import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.shared.Money;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record LedgerEntry(
        UUID id,
        LedgerTransactionId transactionId,
        AccountId accountId,
        EntryDirection direction,
        Money amount,
        Instant createdAt) {

    public LedgerEntry {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(transactionId, "transactionId is required");
        Objects.requireNonNull(accountId, "accountId is required");
        Objects.requireNonNull(direction, "direction is required");
        Objects.requireNonNull(amount, "amount is required");
        Objects.requireNonNull(createdAt, "createdAt is required");
        if (amount.cents() == 0) {
            throw new IllegalArgumentException("Ledger entry amount must be positive");
        }
    }

    public static LedgerEntry debit(LedgerTransactionId transactionId, AccountId accountId, Money amount, Instant now) {
        return new LedgerEntry(UUID.randomUUID(), transactionId, accountId, EntryDirection.DEBIT, amount, now);
    }

    public static LedgerEntry credit(
            LedgerTransactionId transactionId, AccountId accountId, Money amount, Instant now) {
        return new LedgerEntry(UUID.randomUUID(), transactionId, accountId, EntryDirection.CREDIT, amount, now);
    }
}
