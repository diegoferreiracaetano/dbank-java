package com.diegoferreiracaetano.dbank.ledger.application.port.out;

import java.util.function.Supplier;

public interface TransactionRunner {

    void run(Runnable action);

    <T> T call(Supplier<T> action);
}
