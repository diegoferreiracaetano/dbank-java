package com.diegoferreiracaetano.dbank.ledger.adapter.out.persistence.ledger;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataLedgerTransactionRepository extends JpaRepository<LedgerTransactionEntity, UUID> {}
