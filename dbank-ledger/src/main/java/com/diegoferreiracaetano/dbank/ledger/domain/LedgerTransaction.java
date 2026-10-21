package com.diegoferreiracaetano.dbank.ledger.domain;

import java.time.Instant;
import java.util.List;

public record LedgerTransaction(LedgerTransactionId id, List<LedgerEntry> entries) {

    public LedgerTransaction {
        entries = List.copyOf(entries);
        if (entries.size() < 2) {
            throw new IllegalArgumentException("A transaction needs at least two entries");
        }
        if (entries.stream().anyMatch(entry -> !entry.transactionId().equals(id))) {
            throw new IllegalArgumentException("All entries must belong to the transaction " + id.value());
        }
        var debits = total(entries, EntryDirection.DEBIT);
        var credits = total(entries, EntryDirection.CREDIT);
        if (!debits.equals(credits)) {
            throw new UnbalancedTransactionException(debits, credits);
        }
    }

    public static LedgerTransaction transfer(
            LedgerTransactionId id, AccountId from, AccountId to, Money amount, Instant now) {
        if (from.equals(to)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        return new LedgerTransaction(
                id, List.of(LedgerEntry.debit(id, from, amount, now), LedgerEntry.credit(id, to, amount, now)));
    }

    private static Money total(List<LedgerEntry> entries, EntryDirection direction) {
        return entries.stream()
                .filter(entry -> entry.direction() == direction)
                .map(LedgerEntry::amount)
                .reduce(Money.ZERO, Money::plus);
    }
}
