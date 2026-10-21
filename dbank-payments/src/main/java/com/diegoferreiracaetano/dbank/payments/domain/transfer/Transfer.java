package com.diegoferreiracaetano.dbank.payments.domain.transfer;

import com.diegoferreiracaetano.dbank.payments.domain.shared.Money;
import java.time.Instant;
import java.util.Objects;

public record Transfer(
        TransferId id,
        AccountId from,
        AccountId to,
        Money amount,
        TransferStatus status,
        FailureReason failureReason,
        Instant createdAt) {

    public Transfer {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(from, "from is required");
        Objects.requireNonNull(to, "to is required");
        Objects.requireNonNull(amount, "amount is required");
        Objects.requireNonNull(status, "status is required");
        Objects.requireNonNull(createdAt, "createdAt is required");
        if (from.equals(to)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        if (amount.isZero()) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        if ((status == TransferStatus.FAILED) != (failureReason != null)) {
            throw new IllegalArgumentException("A failure reason is required exactly when the transfer failed");
        }
    }

    public static Transfer request(TransferId id, AccountId from, AccountId to, Money amount, Instant now) {
        return new Transfer(id, from, to, amount, TransferStatus.PENDING, null, now);
    }

    public Transfer complete() {
        return switch (status) {
            case PENDING -> new Transfer(id, from, to, amount, TransferStatus.COMPLETED, null, createdAt);
            case COMPLETED -> this;
            case FAILED -> throw new InvalidTransferTransitionException(id, status, "completed");
        };
    }

    public Transfer fail(FailureReason reason) {
        Objects.requireNonNull(reason, "reason is required");
        return switch (status) {
            case PENDING -> new Transfer(id, from, to, amount, TransferStatus.FAILED, reason, createdAt);
            case FAILED -> sameFailure(reason);
            case COMPLETED -> throw new InvalidTransferTransitionException(id, status, "failed with " + reason);
        };
    }

    private Transfer sameFailure(FailureReason reason) {
        if (reason != failureReason) {
            throw new InvalidTransferTransitionException(id, status, "failed with " + reason);
        }
        return this;
    }
}
