package com.diegoferreiracaetano.dbank.ledger.domain.ledger;

import com.diegoferreiracaetano.dbank.ledger.domain.shared.Money;

public class UnbalancedTransactionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UnbalancedTransactionException(Money debits, Money credits) {
        super("Transaction is unbalanced: debits " + debits.cents() + " cents, credits " + credits.cents() + " cents");
    }
}
