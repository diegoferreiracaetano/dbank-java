package com.diegoferreiracaetano.dbank.ledger.domain.account;

import com.diegoferreiracaetano.dbank.ledger.domain.shared.Money;

public class InsufficientFundsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InsufficientFundsException(AccountId accountId, Money balance, Money requested) {
        super("Account " + accountId.value() + " has " + balance.cents() + " cents but " + requested.cents()
                + " were requested");
    }
}
