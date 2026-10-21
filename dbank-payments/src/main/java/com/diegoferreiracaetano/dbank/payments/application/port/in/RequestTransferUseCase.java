package com.diegoferreiracaetano.dbank.payments.application.port.in;

public interface RequestTransferUseCase {

    RequestTransferResult request(RequestTransferCommand command);
}
