package com.diegoferreiracaetano.dbank.ledger.application.service;

import com.diegoferreiracaetano.dbank.ledger.application.port.in.GetAccountUseCase;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.AccountRepository;
import com.diegoferreiracaetano.dbank.ledger.domain.account.Account;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountNotFoundException;

public class GetAccountService implements GetAccountUseCase {

    private final AccountRepository accounts;

    public GetAccountService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public Account get(AccountId id) {
        return accounts.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
    }
}
