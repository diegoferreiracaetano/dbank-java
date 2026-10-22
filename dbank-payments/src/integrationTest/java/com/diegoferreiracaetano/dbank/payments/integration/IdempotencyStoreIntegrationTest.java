package com.diegoferreiracaetano.dbank.payments.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.diegoferreiracaetano.dbank.payments.application.idempotency.ClaimResult;
import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyKey;
import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyRecord;
import com.diegoferreiracaetano.dbank.payments.application.idempotency.RequestFingerprint;
import com.diegoferreiracaetano.dbank.payments.application.port.out.IdempotencyStore;
import com.diegoferreiracaetano.dbank.payments.application.port.out.TransactionRunner;
import com.diegoferreiracaetano.dbank.payments.application.port.out.TransferRepository;
import com.diegoferreiracaetano.dbank.payments.domain.shared.Money;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.AccountId;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.Transfer;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class IdempotencyStoreIntegrationTest extends IntegrationTestBase {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    IdempotencyStore store;

    @Autowired
    TransferRepository transfers;

    @Autowired
    TransactionRunner runner;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void givenNewKey_whenClaiming_thenShouldBeClaimed() {
        var transfer = pending();
        var record = recordFor(transfer);

        var result = runner.run(() -> {
            transfers.save(transfer);
            return store.claim(record);
        });

        assertThat(result).isInstanceOf(ClaimResult.Claimed.class);
    }

    @Test
    void givenExistingKey_whenClaimingAgain_thenShouldReturnTheOriginalRecord() {
        var transfer = pending();
        var original = recordFor(transfer);
        runner.run(() -> {
            transfers.save(transfer);
            return store.claim(original);
        });
        var other = new IdempotencyRecord(
                original.key(),
                RequestFingerprint.of(AccountId.generate(), AccountId.generate(), Money.ofCents(1)),
                TransferId.generate());

        var result = runner.run(() -> store.claim(other));

        assertThat(result).isEqualTo(new ClaimResult.Existing(original));
    }

    @Test
    void givenFailureAfterClaiming_whenRunningInTransaction_thenShouldReleaseTheKey() {
        var transfer = pending();
        var record = recordFor(transfer);

        assertThatThrownBy(() -> runner.run(() -> {
                    transfers.save(transfer);
                    store.claim(record);
                    throw new IllegalStateException("boom");
                }))
                .isInstanceOf(IllegalStateException.class);

        var retry = runner.run(() -> {
            transfers.save(transfer);
            return store.claim(record);
        });
        assertThat(retry).isInstanceOf(ClaimResult.Claimed.class);
    }

    @Test
    void givenKeyForAMissingTransfer_whenCommitting_thenShouldFailOnTheDeferredForeignKey() {
        var record = recordFor(pending());

        assertThatThrownBy(() -> runner.run(() -> store.claim(record)))
                .hasStackTraceContaining("fk_idempotency_key_transfer");

        assertThat(jdbc.queryForObject(
                        "select count(*) from idempotency_key where key = ?",
                        Integer.class,
                        record.key().value()))
                .isZero();
    }

    private Transfer pending() {
        return Transfer.request(
                TransferId.generate(), AccountId.generate(), AccountId.generate(), Money.ofCents(100), NOW);
    }

    private IdempotencyRecord recordFor(Transfer transfer) {
        return new IdempotencyRecord(
                new IdempotencyKey(UUID.randomUUID().toString()),
                RequestFingerprint.of(transfer.from(), transfer.to(), transfer.amount()),
                transfer.id());
    }
}
