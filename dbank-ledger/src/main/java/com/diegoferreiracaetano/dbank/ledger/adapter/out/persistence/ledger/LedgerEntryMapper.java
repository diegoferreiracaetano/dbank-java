package com.diegoferreiracaetano.dbank.ledger.adapter.out.persistence.ledger;

import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerEntry;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransactionId;
import com.diegoferreiracaetano.dbank.ledger.domain.shared.Money;

final class LedgerEntryMapper {

    private LedgerEntryMapper() {}

    static LedgerEntry toDomain(LedgerEntryEntity entity) {
        return new LedgerEntry(
                entity.getId(),
                new LedgerTransactionId(entity.getTransactionId()),
                new AccountId(entity.getAccountId()),
                entity.getDirection(),
                Money.ofCents(entity.getAmountCents()),
                entity.getCreatedAt());
    }
}
