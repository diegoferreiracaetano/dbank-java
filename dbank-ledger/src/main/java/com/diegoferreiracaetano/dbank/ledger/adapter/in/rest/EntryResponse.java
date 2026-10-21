package com.diegoferreiracaetano.dbank.ledger.adapter.in.rest;

import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerEntry;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

record EntryResponse(
        @Schema(description = "Entry id") UUID id,

        @Schema(description = "Transaction that produced this entry")
        UUID transactionId,

        @Schema(
                description = "DEBIT removes money from the account, CREDIT adds it",
                allowableValues = {"DEBIT", "CREDIT"})
        String direction,

        @Schema(description = "Entry amount in cents, always positive", example = "300")
        long amountCents,

        @Schema(description = "When the entry was recorded (UTC)", example = "2026-01-01T00:00:00Z")
        Instant createdAt) {

    static EntryResponse from(LedgerEntry entry) {
        return new EntryResponse(
                entry.id(),
                entry.transactionId().value(),
                entry.direction().name(),
                entry.amount().cents(),
                entry.createdAt());
    }
}
