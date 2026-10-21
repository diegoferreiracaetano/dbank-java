package com.diegoferreiracaetano.dbank.ledger.adapter.out.persistence.ledger;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ledger_transaction")
class LedgerTransactionEntity {

    @Id
    private UUID id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "transaction_id", nullable = false, updatable = false)
    private List<LedgerEntryEntity> entries = new ArrayList<>();

    protected LedgerTransactionEntity() {}

    LedgerTransactionEntity(UUID id, Instant createdAt, List<LedgerEntryEntity> entries) {
        this.id = id;
        this.createdAt = createdAt;
        this.entries = new ArrayList<>(entries);
    }

    UUID getId() {
        return id;
    }

    Instant getCreatedAt() {
        return createdAt;
    }

    List<LedgerEntryEntity> getEntries() {
        return List.copyOf(entries);
    }
}
