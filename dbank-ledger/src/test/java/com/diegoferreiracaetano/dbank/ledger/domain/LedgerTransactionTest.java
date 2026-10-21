package com.diegoferreiracaetano.dbank.ledger.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LedgerTransactionTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final AccountId from = AccountId.generate();
    private final AccountId to = AccountId.generate();

    @Test
    void givenTwoDistinctAccounts_whenTransferring_thenShouldCreateDebitAndCreditOfSameAmount() {
        var id = LedgerTransactionId.generate();

        var transaction = LedgerTransaction.transfer(id, from, to, Money.ofCents(500), NOW);

        assertThat(transaction.entries()).hasSize(2);
        assertThat(transaction.entries()).anySatisfy(entry -> {
            assertThat(entry.direction()).isEqualTo(EntryDirection.DEBIT);
            assertThat(entry.accountId()).isEqualTo(from);
            assertThat(entry.amount()).isEqualTo(Money.ofCents(500));
        });
        assertThat(transaction.entries()).anySatisfy(entry -> {
            assertThat(entry.direction()).isEqualTo(EntryDirection.CREDIT);
            assertThat(entry.accountId()).isEqualTo(to);
            assertThat(entry.amount()).isEqualTo(Money.ofCents(500));
        });
    }

    @Test
    void givenSameAccountOnBothSides_whenTransferring_thenShouldReject() {
        var id = LedgerTransactionId.generate();

        assertThatThrownBy(() -> LedgerTransaction.transfer(id, from, from, Money.ofCents(100), NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenDebitHigherThanCredit_whenCreatingTransaction_thenShouldRejectAsUnbalanced() {
        var id = LedgerTransactionId.generate();
        var entries = List.of(
                LedgerEntry.debit(id, from, Money.ofCents(500), NOW),
                LedgerEntry.credit(id, to, Money.ofCents(400), NOW));

        assertThatThrownBy(() -> new LedgerTransaction(id, entries)).isInstanceOf(UnbalancedTransactionException.class);
    }

    @Test
    void givenCreditsSplitThatSumToDebit_whenCreatingTransaction_thenShouldAccept() {
        var id = LedgerTransactionId.generate();
        var entries = List.of(
                LedgerEntry.debit(id, from, Money.ofCents(500), NOW),
                LedgerEntry.credit(id, to, Money.ofCents(300), NOW),
                LedgerEntry.credit(id, AccountId.generate(), Money.ofCents(200), NOW));

        var transaction = new LedgerTransaction(id, entries);

        assertThat(transaction.entries()).hasSize(3);
    }

    @Test
    void givenSingleEntry_whenCreatingTransaction_thenShouldReject() {
        var id = LedgerTransactionId.generate();
        var entries = List.of(LedgerEntry.debit(id, from, Money.ofCents(500), NOW));

        assertThatThrownBy(() -> new LedgerTransaction(id, entries)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenEntryFromAnotherTransaction_whenCreatingTransaction_thenShouldReject() {
        var id = LedgerTransactionId.generate();
        var entries = List.of(
                LedgerEntry.debit(id, from, Money.ofCents(500), NOW),
                LedgerEntry.credit(LedgerTransactionId.generate(), to, Money.ofCents(500), NOW));

        assertThatThrownBy(() -> new LedgerTransaction(id, entries)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenSourceListCleared_whenReadingEntries_thenShouldKeepOriginalEntries() {
        var id = LedgerTransactionId.generate();
        var entries = new ArrayList<>(List.of(
                LedgerEntry.debit(id, from, Money.ofCents(500), NOW),
                LedgerEntry.credit(id, to, Money.ofCents(500), NOW)));
        var transaction = new LedgerTransaction(id, entries);

        entries.clear();

        assertThat(transaction.entries()).hasSize(2);
    }
}
