package com.diegoferreiracaetano.dbank.ledger.domain.account;

import com.diegoferreiracaetano.dbank.ledger.domain.shared.Money;

public record Account(AccountId id, Money balance, Long version) {

    public Account(AccountId id, Money balance) {
        this(id, balance, null);
    }

    public static Account open() {
        return new Account(AccountId.generate(), Money.ZERO);
    }

    public Account credit(Money amount) {
        return new Account(id, balance.plus(amount), version);
    }

    public Account debit(Money amount) {
        if (!balance.isGreaterThanOrEqualTo(amount)) {
            throw new InsufficientFundsException(id, balance, amount);
        }
        return new Account(id, balance.minus(amount), version);
    }
}
