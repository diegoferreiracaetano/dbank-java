package com.diegoferreiracaetano.dbank.ledger.application.service;

import com.diegoferreiracaetano.dbank.ledger.application.port.in.OpenAccountUseCase;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.AccountRepository;
import com.diegoferreiracaetano.dbank.ledger.domain.account.Account;

public class OpenAccountService implements OpenAccountUseCase {

    private final AccountRepository accounts;

    public OpenAccountService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public Account open() {
        var account = Account.open();
        accounts.save(account);
        return account;
    }
}
