package com.diegoferreiracaetano.dbank.payments.adapter.out.persistence.transfer;

import com.diegoferreiracaetano.dbank.payments.domain.transfer.FailureReason;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.Transfer;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfer")
class TransferEntity {

    @Id
    private UUID id;

    @Column(name = "from_account_id", nullable = false)
    private UUID fromAccountId;

    @Column(name = "to_account_id", nullable = false)
    private UUID toAccountId;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 9)
    private TransferStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason", length = 20)
    private FailureReason failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private Long version;

    protected TransferEntity() {}

    TransferEntity(Transfer transfer) {
        this.id = transfer.id().value();
        this.fromAccountId = transfer.from().value();
        this.toAccountId = transfer.to().value();
        this.amountCents = transfer.amount().cents();
        this.status = transfer.status();
        this.failureReason = transfer.failureReason();
        this.createdAt = transfer.createdAt();
        this.version = transfer.version();
    }

    UUID getId() {
        return id;
    }

    UUID getFromAccountId() {
        return fromAccountId;
    }

    UUID getToAccountId() {
        return toAccountId;
    }

    long getAmountCents() {
        return amountCents;
    }

    TransferStatus getStatus() {
        return status;
    }

    FailureReason getFailureReason() {
        return failureReason;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    Long getVersion() {
        return version;
    }
}
