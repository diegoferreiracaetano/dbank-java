package com.diegoferreiracaetano.dbank.payments.integration;

import static org.assertj.core.api.Assertions.assertThat;

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
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

class IdempotencyCacheIntegrationTest extends IntegrationTestBase {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    IdempotencyStore store;

    @Autowired
    TransferRepository transfers;

    @Autowired
    TransactionRunner runner;

    @Autowired
    StringRedisTemplate redis;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void givenNewKey_whenClaiming_thenShouldNotCacheTheClaim() {
        var record = claimNewKey();

        assertThat(redis.opsForValue().get(cacheKey(record))).isNull();
    }

    @Test
    void givenReplay_whenClaimingAgain_thenShouldCacheTheRecordWithAnExpiry() {
        var record = claimNewKey();
        runner.run(() -> store.claim(record));

        assertThat(redis.opsForValue().get(cacheKey(record))).isNotNull();
        assertThat(redis.getExpire(cacheKey(record), TimeUnit.SECONDS)).isBetween(1L, 86_400L);
    }

    @Test
    void givenCachedKey_whenTheDatabaseRowDisappears_thenShouldStillReplayFromRedis() {
        var record = claimNewKey();
        runner.run(() -> store.claim(record));
        jdbc.update("delete from idempotency_key where key = ?", record.key().value());

        var result = runner.run(() -> store.claim(record));

        assertThat(result).isEqualTo(new ClaimResult.Existing(record));
    }

    private IdempotencyRecord claimNewKey() {
        var transfer = Transfer.request(
                TransferId.generate(), AccountId.generate(), AccountId.generate(), Money.ofCents(100), NOW);
        var record = new IdempotencyRecord(
                new IdempotencyKey(UUID.randomUUID().toString()),
                RequestFingerprint.of(transfer.from(), transfer.to(), transfer.amount()),
                transfer.id());
        runner.run(() -> {
            transfers.save(transfer);
            return store.claim(record);
        });
        return record;
    }

    private static String cacheKey(IdempotencyRecord record) {
        return "idempotency:" + record.key().value();
    }
}
