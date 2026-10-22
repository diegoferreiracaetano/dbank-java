package com.diegoferreiracaetano.dbank.payments.adapter.out.cache;

import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyKey;
import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyRecord;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class FakeCache implements IdempotencyCache {

    private final Map<IdempotencyKey, IdempotencyRecord> records = new HashMap<>();

    FakeCache(IdempotencyRecord... initial) {
        for (var record : initial) {
            records.put(record.key(), record);
        }
    }

    @Override
    public Optional<IdempotencyRecord> find(IdempotencyKey key) {
        return Optional.ofNullable(records.get(key));
    }

    @Override
    public void put(IdempotencyRecord record) {
        records.putIfAbsent(record.key(), record);
    }
}
