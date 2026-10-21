package com.diegoferreiracaetano.dbank.payments.domain.transfer;

public class TransferNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TransferNotFoundException(TransferId id) {
        super("Transfer not found: " + id.value());
    }
}
