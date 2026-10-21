package com.diegoferreiracaetano.dbank.ledger.application.port.out;

public class ConcurrentUpdateException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConcurrentUpdateException(Throwable cause) {
        super("The data was changed by another transaction; retry the operation", cause);
    }
}
