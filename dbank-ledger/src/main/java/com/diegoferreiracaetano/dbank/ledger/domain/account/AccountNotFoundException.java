package com.diegoferreiracaetano.dbank.ledger.domain.account;

public class AccountNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AccountNotFoundException(AccountId accountId) {
        super("Account not found: " + accountId.value());
    }
}
