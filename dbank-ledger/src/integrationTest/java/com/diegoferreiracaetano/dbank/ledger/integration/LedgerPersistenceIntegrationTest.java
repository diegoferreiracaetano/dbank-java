package com.diegoferreiracaetano.dbank.ledger.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegoferreiracaetano.dbank.ledger.application.port.out.AccountRepository;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.LedgerEntryRepository;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.LedgerTransactionRepository;
import com.diegoferreiracaetano.dbank.ledger.domain.account.Account;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.EntryDirection;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerEntry;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransaction;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransactionId;
import com.diegoferreiracaetano.dbank.ledger.domain.shared.Money;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class LedgerPersistenceIntegrationTest extends IntegrationTestBase {

    private static final Instant FIRST = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant SECOND = Instant.parse("2026-01-02T00:00:00Z");
    private static final Instant THIRD = Instant.parse("2026-01-03T00:00:00Z");

    @Autowired
    AccountRepository accounts;

    @Autowired
    LedgerTransactionRepository transactions;

    @Autowired
    LedgerEntryRepository entries;

    @Autowired
    JdbcTemplate jdbc;

    private final AccountId from = AccountId.generate();
    private final AccountId to = AccountId.generate();

    @Test
    void givenTransfer_whenSaving_thenShouldStoreTheTransactionAndBothEntries() {
        openAccounts();
        var id = LedgerTransactionId.generate();

        transactions.save(LedgerTransaction.transfer(id, from, to, Money.ofCents(500), FIRST));

        assertThat(transactions.existsById(id)).isTrue();
        var createdAt = jdbc.queryForObject(
                "select created_at from ledger_transaction where id = ?", Timestamp.class, id.value());
        assertThat(createdAt.toInstant()).isEqualTo(FIRST);
        assertThat(directionOf(id, from)).isEqualTo("DEBIT");
        assertThat(directionOf(id, to)).isEqualTo("CREDIT");
        assertThat(count(id)).isEqualTo(2);
    }

    @Test
    void givenUnknownTransactionId_whenCheckingExistence_thenShouldBeFalse() {
        assertThat(transactions.existsById(LedgerTransactionId.generate())).isFalse();
    }

    @Test
    void givenSavedTransfer_whenReadingTheStatement_thenShouldMapAllFields() {
        openAccounts();
        var id = LedgerTransactionId.generate();
        transactions.save(LedgerTransaction.transfer(id, from, to, Money.ofCents(500), FIRST));

        var statement = entries.findByAccountId(to, 0, 10);

        assertThat(statement).hasSize(1);
        var entry = statement.get(0);
        assertThat(entry.transactionId()).isEqualTo(id);
        assertThat(entry.accountId()).isEqualTo(to);
        assertThat(entry.direction()).isEqualTo(EntryDirection.CREDIT);
        assertThat(entry.amount()).isEqualTo(Money.ofCents(500));
        assertThat(entry.createdAt()).isEqualTo(FIRST);
    }

    @Test
    void givenSeveralTransfers_whenReadingTheStatement_thenShouldReturnNewestFirst() {
        openAccounts();
        postTransfer(FIRST);
        postTransfer(THIRD);
        postTransfer(SECOND);

        var statement = entries.findByAccountId(from, 0, 10);

        assertThat(statement).extracting(LedgerEntry::createdAt).containsExactly(THIRD, SECOND, FIRST);
    }

    @Test
    void givenSeveralTransfers_whenPagingTheStatement_thenShouldSplitTheOrderedEntries() {
        openAccounts();
        postTransfer(FIRST);
        postTransfer(THIRD);
        postTransfer(SECOND);

        var firstPage = entries.findByAccountId(from, 0, 2);
        var secondPage = entries.findByAccountId(from, 1, 2);

        assertThat(firstPage).extracting(LedgerEntry::createdAt).containsExactly(THIRD, SECOND);
        assertThat(secondPage).extracting(LedgerEntry::createdAt).containsExactly(FIRST);
    }

    private void openAccounts() {
        accounts.save(new Account(from, Money.ofCents(10_000)));
        accounts.save(new Account(to, Money.ZERO));
    }

    private void postTransfer(Instant at) {
        transactions.save(LedgerTransaction.transfer(LedgerTransactionId.generate(), from, to, Money.ofCents(100), at));
    }

    private String directionOf(LedgerTransactionId id, AccountId account) {
        return jdbc.queryForObject(
                "select direction from ledger_entry where transaction_id = ? and account_id = ?",
                String.class,
                id.value(),
                account.value());
    }

    private Integer count(LedgerTransactionId id) {
        return jdbc.queryForObject(
                "select count(*) from ledger_entry where transaction_id = ?", Integer.class, id.value());
    }
}
