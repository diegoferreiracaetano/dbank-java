package com.diegoferreiracaetano.dbank.payments.integration;

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
    void givenZeroAmount_whenInsertingTransfer_thenShouldViolateTheCheck() {
        assertThatThrownBy(() ->
                        insertTransfer(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 0, "PENDING", null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_transfer_amount_positive");
    }

    @Test
    void givenSameAccountOnBothSides_whenInsertingTransfer_thenShouldViolateTheCheck() {
        var account = UUID.randomUUID();

        assertThatThrownBy(() -> insertTransfer(UUID.randomUUID(), account, account, 100, "PENDING", null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_transfer_distinct_accounts");
    }

    @Test
    void givenUnknownStatus_whenInsertingTransfer_thenShouldViolateTheCheck() {
        assertThatThrownBy(() ->
                        insertTransfer(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 100, "UNKNOWN", null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_transfer_status");
    }

    @Test
    void givenUnknownFailureReason_whenInsertingTransfer_thenShouldViolateTheCheck() {
        assertThatThrownBy(() -> insertTransfer(
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 100, "FAILED", "SOMETHING"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_transfer_failure_reason");
    }

    @Test
    void givenFailedWithoutReason_whenInsertingTransfer_thenShouldViolateTheCheck() {
        assertThatThrownBy(() ->
                        insertTransfer(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 100, "FAILED", null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_transfer_reason_iff_failed");
    }

    @Test
    void givenPendingWithReason_whenInsertingTransfer_thenShouldViolateTheCheck() {
        assertThatThrownBy(() -> insertTransfer(
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 100, "PENDING", "INSUFFICIENT_FUNDS"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_transfer_reason_iff_failed");
    }

    @Test
    void givenDuplicateKey_whenInsertingIdempotencyKey_thenShouldViolateThePrimaryKey() {
        var transferId = UUID.randomUUID();
        insertTransfer(transferId, UUID.randomUUID(), UUID.randomUUID(), 100, "PENDING", null);
        var key = UUID.randomUUID().toString();
        insertKey(key, transferId);

        assertThatThrownBy(() -> insertKey(key, transferId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("idempotency_key_pkey");
    }

    @Test
    void givenKeyForAMissingTransfer_whenInsertingIdempotencyKey_thenShouldViolateTheForeignKeyOnCommit() {
        assertThatThrownBy(() -> insertKey(UUID.randomUUID().toString(), UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_idempotency_key_transfer");
    }

    private void insertTransfer(UUID id, UUID from, UUID to, long amountCents, String status, String failureReason) {
        jdbc.update(
                "insert into transfer (id, from_account_id, to_account_id, amount_cents, status, failure_reason,"
                        + " created_at) values (?, ?, ?, ?, ?, ?, now())",
                id,
                from,
                to,
                amountCents,
                status,
                failureReason);
    }

    private void insertKey(String key, UUID transferId) {
        jdbc.update(
                "insert into idempotency_key (key, fingerprint, transfer_id) values (?, ?, ?)",
                key,
                "f".repeat(64),
                transferId);
    }
}
