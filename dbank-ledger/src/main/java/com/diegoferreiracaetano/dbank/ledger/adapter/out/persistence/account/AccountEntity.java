package com.diegoferreiracaetano.dbank.ledger.adapter.out.persistence.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

@Entity
@Table(name = "account")
class AccountEntity {

    @Id
    private UUID id;

    @Column(name = "balance_cents", nullable = false)
    private long balanceCents;

    @Version
    private Long version;

    protected AccountEntity() {}

    AccountEntity(UUID id, long balanceCents, Long version) {
        this.id = id;
        this.balanceCents = balanceCents;
        this.version = version;
    }

    UUID getId() {
        return id;
    }

    long getBalanceCents() {
        return balanceCents;
    }

    Long getVersion() {
        return version;
    }
}
