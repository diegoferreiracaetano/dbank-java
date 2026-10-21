package com.diegoferreiracaetano.dbank.ledger.adapter.out.persistence.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "account")
class AccountEntity {

    @Id
    private UUID id;

    @Column(name = "balance_cents", nullable = false)
    private long balanceCents;

    protected AccountEntity() {}

    AccountEntity(UUID id, long balanceCents) {
        this.id = id;
        this.balanceCents = balanceCents;
    }

    UUID getId() {
        return id;
    }

    long getBalanceCents() {
        return balanceCents;
    }
}
