package com.diegoferreiracaetano.dbank.payments.adapter.in.rest;

import com.diegoferreiracaetano.dbank.payments.domain.transfer.Transfer;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

record TransferResponse(
        @Schema(description = "Transfer id") UUID id,
        @Schema(description = "Account debited") UUID fromAccountId,
        @Schema(description = "Account credited") UUID toAccountId,

        @Schema(description = "Amount in cents", example = "1000")
        long amountCents,

        @Schema(
                description = "PENDING until the ledger answers",
                allowableValues = {"PENDING", "COMPLETED", "FAILED"})
        String status,

        @Schema(description = "Present only when the transfer failed", nullable = true)
        String failureReason,

        @Schema(description = "When the transfer was requested (UTC)", example = "2026-01-01T00:00:00Z")
        Instant createdAt) {

    static TransferResponse from(Transfer transfer) {
        return new TransferResponse(
                transfer.id().value(),
                transfer.from().value(),
                transfer.to().value(),
                transfer.amount().cents(),
                transfer.status().name(),
                transfer.failureReason() == null
                        ? null
                        : transfer.failureReason().name(),
                transfer.createdAt());
    }
}
