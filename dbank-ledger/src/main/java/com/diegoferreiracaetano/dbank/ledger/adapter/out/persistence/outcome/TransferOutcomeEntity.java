package com.diegoferreiracaetano.dbank.ledger.adapter.out.persistence.outcome;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfer_outcome")
class TransferOutcomeEntity {

    @Id
    @Column(name = "transfer_id")
    private UUID transferId;

    @Column(nullable = false, length = 8)
    private String status;

    @Column(length = 20)
    private String reason;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected TransferOutcomeEntity() {}

    TransferOutcomeEntity(UUID transferId, String status, String reason, Instant processedAt) {
        this.transferId = transferId;
        this.status = status;
        this.reason = reason;
        this.processedAt = processedAt;
    }

    UUID getTransferId() {
        return transferId;
    }

    String getStatus() {
        return status;
    }

    String getReason() {
        return reason;
    }

    Instant getProcessedAt() {
        return processedAt;
    }
}
