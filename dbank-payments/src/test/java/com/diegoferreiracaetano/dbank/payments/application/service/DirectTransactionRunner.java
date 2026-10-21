package com.diegoferreiracaetano.dbank.payments.application.service;

import com.diegoferreiracaetano.dbank.payments.application.port.out.TransactionRunner;
import java.util.function.Supplier;

class DirectTransactionRunner implements TransactionRunner {

    @Override
    public <T> T run(Supplier<T> action) {
        return action.get();
    }
}
