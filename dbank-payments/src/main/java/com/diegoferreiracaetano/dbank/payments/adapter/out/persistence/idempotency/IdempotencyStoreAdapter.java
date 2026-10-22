package com.diegoferreiracaetano.dbank.payments.adapter.out.persistence.idempotency;

import com.diegoferreiracaetano.dbank.payments.application.idempotency.ClaimResult;
import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyKey;
import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyRecord;
import com.diegoferreiracaetano.dbank.payments.application.idempotency.RequestFingerprint;
import com.diegoferreiracaetano.dbank.payments.application.port.out.IdempotencyStore;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferId;
import org.springframework.stereotype.Component;

@Component
class IdempotencyStoreAdapter implements IdempotencyStore {

    private final SpringDataIdempotencyKeyRepository repository;

    IdempotencyStoreAdapter(SpringDataIdempotencyKeyRepository repository) {
        this.repository = repository;
    }

    @Override
    public ClaimResult claim(IdempotencyRecord candidate) {
        var inserted = repository.insertIfAbsent(
                candidate.key().value(),
                candidate.fingerprint().value(),
                candidate.transferId().value());
        if (inserted == 1) {
            return new ClaimResult.Claimed();
        }
        var existing = repository.findById(candidate.key().value()).orElseThrow();
        return new ClaimResult.Existing(toRecord(existing));
    }

    private static IdempotencyRecord toRecord(IdempotencyKeyEntity entity) {
        return new IdempotencyRecord(
                new IdempotencyKey(entity.getIdempotencyKey()),
                new RequestFingerprint(entity.getFingerprint()),
                new TransferId(entity.getTransferId()));
    }
}
