package com.diegoferreiracaetano.dbank.ledger.application.port.out;

public interface TransactionRunner {

    void run(Runnable action);
}
