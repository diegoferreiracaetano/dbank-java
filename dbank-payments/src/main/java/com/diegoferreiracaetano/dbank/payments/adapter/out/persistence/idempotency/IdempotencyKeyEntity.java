package com.diegoferreiracaetano.dbank.payments.adapter.out.persistence.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "idempotency_key")
class IdempotencyKeyEntity {

    @Id
    @Column(name = "key", length = 255)
    private String idempotencyKey;

    @Column(nullable = false, length = 64)
    private String fingerprint;

    @Column(name = "transfer_id", nullable = false)
    private UUID transferId;

    protected IdempotencyKeyEntity() {}

    String getIdempotencyKey() {
        return idempotencyKey;
    }

    String getFingerprint() {
        return fingerprint;
    }

    UUID getTransferId() {
        return transferId;
    }
}
