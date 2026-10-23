package com.diegoferreiracaetano.dbank.ledger.adapter.out.persistence.outcome;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataTransferOutcomeRepository extends JpaRepository<TransferOutcomeEntity, UUID> {}
