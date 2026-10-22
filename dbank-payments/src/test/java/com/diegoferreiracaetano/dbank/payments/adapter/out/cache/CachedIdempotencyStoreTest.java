package com.diegoferreiracaetano.dbank.payments.adapter.out.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegoferreiracaetano.dbank.payments.application.idempotency.ClaimResult;
import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyKey;
import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyRecord;
import com.diegoferreiracaetano.dbank.payments.application.idempotency.RequestFingerprint;
import com.diegoferreiracaetano.dbank.payments.application.port.out.IdempotencyStore;
import com.diegoferreiracaetano.dbank.payments.domain.shared.Money;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.AccountId;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferId;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CachedIdempotencyStoreTest {

    private final IdempotencyRecord record = new IdempotencyRecord(
            new IdempotencyKey(UUID.randomUUID().toString()),
            RequestFingerprint.of(AccountId.generate(), AccountId.generate(), Money.ofCents(100)),
            TransferId.generate());

    @Test
    void givenCachedKey_whenClaiming_thenShouldReturnItWithoutTouchingTheDatabase() {
        var databaseCalls = new AtomicInteger();
        IdempotencyStore database = candidate -> {
            databaseCalls.incrementAndGet();
            return new ClaimResult.Claimed();
        };
        var store = new CachedIdempotencyStore(database, new FakeCache(record));

        var result = store.claim(record);

        assertThat(result).isEqualTo(new ClaimResult.Existing(record));
        assertThat(databaseCalls).hasValue(0);
    }

    @Test
    void givenCacheMissAndNewKey_whenClaiming_thenShouldClaimInTheDatabaseAndNotCache() {
        var cache = new FakeCache();
        var store = new CachedIdempotencyStore(candidate -> new ClaimResult.Claimed(), cache);

        var result = store.claim(record);

        assertThat(result).isInstanceOf(ClaimResult.Claimed.class);
        assertThat(cache.find(record.key())).isEmpty();
    }

    @Test
    void givenCacheMissAndExistingKey_whenClaiming_thenShouldReturnTheExistingAndCacheIt() {
        var cache = new FakeCache();
        var store = new CachedIdempotencyStore(candidate -> new ClaimResult.Existing(record), cache);

        var result = store.claim(record);

        assertThat(result).isEqualTo(new ClaimResult.Existing(record));
        assertThat(cache.find(record.key())).contains(record);
    }

    @Test
    void givenBrokenCache_whenClaiming_thenShouldFallBackToTheDatabase() {
        var store = new CachedIdempotencyStore(candidate -> new ClaimResult.Claimed(), new BrokenCache());

        var result = store.claim(record);

        assertThat(result).isInstanceOf(ClaimResult.Claimed.class);
    }

    @Test
    void givenBrokenCacheOnWrite_whenReplaying_thenShouldStillReturnTheExisting() {
        var store = new CachedIdempotencyStore(candidate -> new ClaimResult.Existing(record), new BrokenCache());

        var result = store.claim(record);

        assertThat(result).isEqualTo(new ClaimResult.Existing(record));
    }
}
