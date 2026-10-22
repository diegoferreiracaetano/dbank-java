package com.diegoferreiracaetano.dbank.payments.adapter.out.persistence.transfer;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataTransferRepository extends JpaRepository<TransferEntity, UUID> {}
