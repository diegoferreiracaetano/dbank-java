package com.diegoferreiracaetano.dbank.payments.application.port.out;

import java.util.function.Supplier;

public interface TransactionRunner {

    <T> T run(Supplier<T> action);
}
