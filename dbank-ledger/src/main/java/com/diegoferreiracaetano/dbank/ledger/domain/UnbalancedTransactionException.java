package com.diegoferreiracaetano.dbank.ledger.domain;

public class UnbalancedTransactionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UnbalancedTransactionException(Money debits, Money credits) {
        super("Transaction is unbalanced: debits " + debits.cents() + " cents, credits " + credits.cents() + " cents");
    }
}
