package com.diegoferreiracaetano.dbank.ledger.adapter.out.persistence.ledger;

import com.diegoferreiracaetano.dbank.ledger.application.port.out.LedgerTransactionRepository;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransaction;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransactionId;
import org.springframework.stereotype.Component;

@Component
class LedgerTransactionPersistenceAdapter implements LedgerTransactionRepository {

    private final SpringDataLedgerTransactionRepository repository;

    LedgerTransactionPersistenceAdapter(SpringDataLedgerTransactionRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsById(LedgerTransactionId id) {
        return repository.existsById(id.value());
    }

    @Override
    public void save(LedgerTransaction transaction) {
        repository.save(LedgerTransactionMapper.toEntity(transaction));
    }
}
