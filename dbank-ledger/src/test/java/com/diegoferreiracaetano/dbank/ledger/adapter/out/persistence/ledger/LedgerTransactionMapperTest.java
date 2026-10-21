package com.diegoferreiracaetano.dbank.ledger.adapter.out.persistence.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.EntryDirection;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransaction;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransactionId;
import com.diegoferreiracaetano.dbank.ledger.domain.shared.Money;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class LedgerTransactionMapperTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final AccountId from = AccountId.generate();
    private final AccountId to = AccountId.generate();

    @Test
    void givenTransfer_whenMappingToEntity_thenShouldKeepIdAndCreateTwoEntries() {
        var id = LedgerTransactionId.generate();
        var transaction = LedgerTransaction.transfer(id, from, to, Money.ofCents(500), NOW);

        var entity = LedgerTransactionMapper.toEntity(transaction);

        assertThat(entity.getId()).isEqualTo(id.value());
        assertThat(entity.getCreatedAt()).isEqualTo(NOW);
        assertThat(entity.getEntries()).hasSize(2);
    }

    @Test
    void givenTransfer_whenMappingToEntity_thenShouldKeepDirectionsAccountsAndAmounts() {
        var transaction = LedgerTransaction.transfer(LedgerTransactionId.generate(), from, to, Money.ofCents(500), NOW);

        var entity = LedgerTransactionMapper.toEntity(transaction);

        assertThat(entity.getEntries()).anySatisfy(entry -> {
            assertThat(entry.getDirection()).isEqualTo(EntryDirection.DEBIT);
            assertThat(entry.getAccountId()).isEqualTo(from.value());
            assertThat(entry.getAmountCents()).isEqualTo(500);
        });
        assertThat(entity.getEntries()).anySatisfy(entry -> {
            assertThat(entry.getDirection()).isEqualTo(EntryDirection.CREDIT);
            assertThat(entry.getAccountId()).isEqualTo(to.value());
            assertThat(entry.getAmountCents()).isEqualTo(500);
        });
    }
}
