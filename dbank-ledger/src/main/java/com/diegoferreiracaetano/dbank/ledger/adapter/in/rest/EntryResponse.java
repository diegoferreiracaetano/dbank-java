package com.diegoferreiracaetano.dbank.ledger.adapter.in.rest;

import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerEntry;
import java.time.Instant;
import java.util.UUID;

record EntryResponse(UUID id, UUID transactionId, String direction, long amountCents, Instant createdAt) {

    static EntryResponse from(LedgerEntry entry) {
        return new EntryResponse(
                entry.id(),
                entry.transactionId().value(),
                entry.direction().name(),
                entry.amount().cents(),
                entry.createdAt());
    }
}
