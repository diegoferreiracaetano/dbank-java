package com.diegoferreiracaetano.dbank.payments.adapter.out.cache;

import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyKey;
import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyRecord;
import java.util.Optional;

class BrokenCache implements IdempotencyCache {

    @Override
    public Optional<IdempotencyRecord> find(IdempotencyKey key) {
        throw new IllegalStateException("cache is down");
    }

    @Override
    public void put(IdempotencyRecord record) {
        throw new IllegalStateException("cache is down");
    }
}
