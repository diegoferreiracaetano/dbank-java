package com.diegoferreiracaetano.dbank.payments.domain.transfer;

public class InvalidTransferTransitionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidTransferTransitionException(TransferId id, TransferStatus current, String attempted) {
        super("Transfer " + id.value() + " is " + current + " and cannot be " + attempted);
    }
}
