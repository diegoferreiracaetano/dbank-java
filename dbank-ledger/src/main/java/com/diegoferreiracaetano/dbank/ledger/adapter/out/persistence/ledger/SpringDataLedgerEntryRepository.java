package com.diegoferreiracaetano.dbank.ledger.adapter.out.persistence.ledger;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataLedgerEntryRepository extends JpaRepository<LedgerEntryEntity, UUID> {

    List<LedgerEntryEntity> findByAccountIdOrderByCreatedAtDescIdDesc(UUID accountId, Pageable pageable);
}
