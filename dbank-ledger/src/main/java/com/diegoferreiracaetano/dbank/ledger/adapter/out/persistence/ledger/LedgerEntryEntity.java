package com.diegoferreiracaetano.dbank.ledger.adapter.out.persistence.ledger;

import com.diegoferreiracaetano.dbank.ledger.domain.ledger.EntryDirection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entry")
class LedgerEntryEntity {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6)
    private EntryDirection direction;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LedgerEntryEntity() {}

    LedgerEntryEntity(UUID id, UUID accountId, EntryDirection direction, long amountCents, Instant createdAt) {
        this.id = id;
        this.accountId = accountId;
        this.direction = direction;
        this.amountCents = amountCents;
        this.createdAt = createdAt;
    }

    UUID getId() {
        return id;
    }

    UUID getAccountId() {
        return accountId;
    }

    EntryDirection getDirection() {
        return direction;
    }

    long getAmountCents() {
        return amountCents;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
