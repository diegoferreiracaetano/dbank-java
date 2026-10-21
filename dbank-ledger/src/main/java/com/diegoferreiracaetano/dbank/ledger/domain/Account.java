package com.diegoferreiracaetano.dbank.ledger.domain;

public record Account(AccountId id, Money balance) {

    public static Account open() {
        return new Account(AccountId.generate(), Money.ZERO);
    }

    public Account credit(Money amount) {
        return new Account(id, balance.plus(amount));
    }

    public Account debit(Money amount) {
        if (!balance.isGreaterThanOrEqualTo(amount)) {
            throw new InsufficientFundsException(id, balance, amount);
        }
        return new Account(id, balance.minus(amount));
    }
}
