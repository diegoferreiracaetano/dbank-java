package com.diegoferreiracaetano.dbank.ledger.application.service;

import com.diegoferreiracaetano.dbank.ledger.application.port.out.TransactionRunner;

class DirectTransactionRunner implements TransactionRunner {

    @Override
    public void run(Runnable action) {
        action.run();
    }
}
