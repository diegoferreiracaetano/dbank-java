package com.diegoferreiracaetano.dbank.ledger.adapter.out.persistence.account;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAccountRepository extends JpaRepository<AccountEntity, UUID> {}
