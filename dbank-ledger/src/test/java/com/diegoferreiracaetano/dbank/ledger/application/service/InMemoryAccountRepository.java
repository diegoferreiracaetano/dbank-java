package com.diegoferreiracaetano.dbank.ledger.application.service;

import com.diegoferreiracaetano.dbank.ledger.application.port.out.AccountRepository;
import com.diegoferreiracaetano.dbank.ledger.domain.account.Account;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class InMemoryAccountRepository implements AccountRepository {

    private final Map<AccountId, Account> accounts = new HashMap<>();

    @Override
    public Optional<Account> findById(AccountId id) {
        return Optional.ofNullable(accounts.get(id));
    }

    @Override
    public void save(Account account) {
        accounts.put(account.id(), account);
    }
}
