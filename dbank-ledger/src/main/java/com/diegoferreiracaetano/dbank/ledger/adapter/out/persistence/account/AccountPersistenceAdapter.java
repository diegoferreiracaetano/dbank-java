package com.diegoferreiracaetano.dbank.ledger.adapter.out.persistence.account;

import com.diegoferreiracaetano.dbank.ledger.application.port.out.AccountRepository;
import com.diegoferreiracaetano.dbank.ledger.domain.account.Account;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class AccountPersistenceAdapter implements AccountRepository {

    private final SpringDataAccountRepository repository;

    AccountPersistenceAdapter(SpringDataAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Account> findById(AccountId id) {
        return repository.findById(id.value()).map(AccountMapper::toDomain);
    }

    @Override
    public void save(Account account) {
        repository.save(AccountMapper.toEntity(account));
    }
}
