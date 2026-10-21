package com.diegoferreiracaetano.dbank.ledger.integration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class SchemaConstraintsIntegrationTest extends IntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void givenNegativeBalance_whenInsertingAccount_thenShouldViolateTheCheck() {
        assertThatThrownBy(() ->
                        jdbc.update("insert into account (id, balance_cents) values (?, ?)", UUID.randomUUID(), -1))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_account_balance_non_negative");
    }

    @Test
    void givenDuplicateTransactionId_whenInsertingTransaction_thenShouldViolateThePrimaryKey() {
        var id = insertTransaction();

        assertThatThrownBy(() -> jdbc.update("insert into ledger_transaction (id, created_at) values (?, now())", id))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ledger_transaction_pkey");
    }

    @Test
    void givenZeroAmount_whenInsertingEntry_thenShouldViolateTheCheck() {
        var accountId = insertAccount();
        var transactionId = insertTransaction();

        assertThatThrownBy(() -> insertEntry(transactionId, accountId, "DEBIT", 0))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_ledger_entry_amount_positive");
    }

    @Test
    void givenInvalidDirection_whenInsertingEntry_thenShouldViolateTheCheck() {
        var accountId = insertAccount();
        var transactionId = insertTransaction();

        assertThatThrownBy(() -> insertEntry(transactionId, accountId, "DEBITS", 10))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_ledger_entry_direction");
    }

    @Test
    void givenUnknownAccount_whenInsertingEntry_thenShouldViolateTheForeignKey() {
        var transactionId = insertTransaction();

        assertThatThrownBy(() -> insertEntry(transactionId, UUID.randomUUID(), "DEBIT", 10))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ledger_entry_account_id_fkey");
    }

    @Test
    void givenUnknownTransaction_whenInsertingEntry_thenShouldViolateTheForeignKey() {
        var accountId = insertAccount();

        assertThatThrownBy(() -> insertEntry(UUID.randomUUID(), accountId, "DEBIT", 10))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ledger_entry_transaction_id_fkey");
    }

    private UUID insertAccount() {
        var id = UUID.randomUUID();
        jdbc.update("insert into account (id, balance_cents) values (?, 100)", id);
        return id;
    }

    private UUID insertTransaction() {
        var id = UUID.randomUUID();
        jdbc.update("insert into ledger_transaction (id, created_at) values (?, now())", id);
        return id;
    }

    private void insertEntry(UUID transactionId, UUID accountId, String direction, long amount) {
        jdbc.update(
                "insert into ledger_entry (id, transaction_id, account_id, direction, amount_cents, created_at)"
                        + " values (?, ?, ?, ?, ?, now())",
                UUID.randomUUID(),
                transactionId,
                accountId,
                direction,
                amount);
    }
}
