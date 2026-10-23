package com.diegoferreiracaetano.dbank.ledger.application.service;

import com.diegoferreiracaetano.dbank.ledger.application.port.out.TransactionRunner;
import java.util.function.Supplier;

class DirectTransactionRunner implements TransactionRunner {

    @Override
    public void run(Runnable action) {
        action.run();
    }

    @Override
    public <T> T call(Supplier<T> action) {
        return action.get();
    }
}
