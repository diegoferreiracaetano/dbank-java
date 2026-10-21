package com.diegoferreiracaetano.dbank.ledger.adapter.out.persistence.ledger;

import com.diegoferreiracaetano.dbank.ledger.application.port.out.LedgerEntryRepository;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerEntry;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
class LedgerEntryPersistenceAdapter implements LedgerEntryRepository {

    private final SpringDataLedgerEntryRepository repository;

    LedgerEntryPersistenceAdapter(SpringDataLedgerEntryRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<LedgerEntry> findByAccountId(AccountId id, int page, int size) {
        return repository.findByAccountIdOrderByCreatedAtDescIdDesc(id.value(), PageRequest.of(page, size)).stream()
                .map(LedgerEntryMapper::toDomain)
                .toList();
    }
}
