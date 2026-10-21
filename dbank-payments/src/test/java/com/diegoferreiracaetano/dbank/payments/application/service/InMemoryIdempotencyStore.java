package com.diegoferreiracaetano.dbank.payments.application.service;

import com.diegoferreiracaetano.dbank.payments.application.idempotency.ClaimResult;
import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyKey;
import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyRecord;
import com.diegoferreiracaetano.dbank.payments.application.port.out.IdempotencyStore;
import java.util.HashMap;
import java.util.Map;

class InMemoryIdempotencyStore implements IdempotencyStore {

    private final Map<IdempotencyKey, IdempotencyRecord> records = new HashMap<>();

    @Override
    public ClaimResult claim(IdempotencyRecord candidate) {
        var existing = records.putIfAbsent(candidate.key(), candidate);
        return existing == null ? new ClaimResult.Claimed() : new ClaimResult.Existing(existing);
    }

    int size() {
        return records.size();
    }
}
