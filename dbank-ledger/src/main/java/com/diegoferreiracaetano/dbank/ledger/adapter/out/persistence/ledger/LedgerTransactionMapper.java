package com.diegoferreiracaetano.dbank.ledger.adapter.out.persistence.ledger;

import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerEntry;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransaction;
import java.util.Comparator;

final class LedgerTransactionMapper {

    private LedgerTransactionMapper() {}

    static LedgerTransactionEntity toEntity(LedgerTransaction transaction) {
        var entries = transaction.entries().stream()
                .map(LedgerTransactionMapper::toEntryEntity)
                .toList();
        var createdAt = transaction.entries().stream()
                .map(LedgerEntry::createdAt)
                .min(Comparator.naturalOrder())
                .orElseThrow();
        return new LedgerTransactionEntity(transaction.id().value(), createdAt, entries);
    }

    private static LedgerEntryEntity toEntryEntity(LedgerEntry entry) {
        return new LedgerEntryEntity(
                entry.id(),
                entry.accountId().value(),
                entry.direction(),
                entry.amount().cents(),
                entry.createdAt());
    }
}
