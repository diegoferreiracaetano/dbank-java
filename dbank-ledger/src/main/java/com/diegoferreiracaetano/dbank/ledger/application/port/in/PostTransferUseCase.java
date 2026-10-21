package com.diegoferreiracaetano.dbank.ledger.application.port.in;

public interface PostTransferUseCase {

    void post(PostTransferCommand command);
}
