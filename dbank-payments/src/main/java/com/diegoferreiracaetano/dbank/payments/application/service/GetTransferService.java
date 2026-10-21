package com.diegoferreiracaetano.dbank.payments.application.service;

import com.diegoferreiracaetano.dbank.payments.application.port.in.GetTransferUseCase;
import com.diegoferreiracaetano.dbank.payments.application.port.out.TransferRepository;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.Transfer;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferId;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferNotFoundException;

public class GetTransferService implements GetTransferUseCase {

    private final TransferRepository transfers;

    public GetTransferService(TransferRepository transfers) {
        this.transfers = transfers;
    }

    @Override
    public Transfer get(TransferId id) {
        return transfers.findById(id).orElseThrow(() -> new TransferNotFoundException(id));
    }
}
