package com.diegoferreiracaetano.dbank.payments.adapter.out.cache;

import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyKey;
import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyRecord;
import java.util.Optional;

interface IdempotencyCache {

    Optional<IdempotencyRecord> find(IdempotencyKey key);

    void put(IdempotencyRecord record);
}
