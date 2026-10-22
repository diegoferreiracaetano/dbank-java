package com.diegoferreiracaetano.dbank.payments.adapter.out.cache;

import com.diegoferreiracaetano.dbank.payments.application.idempotency.ClaimResult;
import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyKey;
import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyRecord;
import com.diegoferreiracaetano.dbank.payments.application.port.out.IdempotencyStore;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
class CachedIdempotencyStore implements IdempotencyStore {

    private static final Logger LOG = LoggerFactory.getLogger(CachedIdempotencyStore.class);

    private final IdempotencyStore database;
    private final IdempotencyCache cache;

    CachedIdempotencyStore(@Qualifier("databaseIdempotencyStore") IdempotencyStore database, IdempotencyCache cache) {
        this.database = database;
        this.cache = cache;
    }

    @Override
    public ClaimResult claim(IdempotencyRecord candidate) {
        var cached = findInCache(candidate.key());
        if (cached.isPresent()) {
            return new ClaimResult.Existing(cached.get());
        }
        var result = database.claim(candidate);
        if (result instanceof ClaimResult.Existing existing) {
            putInCache(existing.record());
        }
        return result;
    }

    private Optional<IdempotencyRecord> findInCache(IdempotencyKey key) {
        try {
            return cache.find(key);
        } catch (RuntimeException e) {
            LOG.warn("Idempotency cache unavailable, falling back to the database", e);
            return Optional.empty();
        }
    }

    private void putInCache(IdempotencyRecord record) {
        try {
            cache.put(record);
        } catch (RuntimeException e) {
            LOG.warn("Idempotency cache could not be updated", e);
        }
    }
}
