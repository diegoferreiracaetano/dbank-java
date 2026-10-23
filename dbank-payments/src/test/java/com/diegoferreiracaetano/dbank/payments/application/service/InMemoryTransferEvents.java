package com.diegoferreiracaetano.dbank.payments.application.service;

import com.diegoferreiracaetano.dbank.payments.application.port.out.TransferEvents;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.Transfer;
import java.util.ArrayList;
import java.util.List;

class InMemoryTransferEvents implements TransferEvents {

    private final List<Transfer> requested = new ArrayList<>();

    @Override
    public void transferRequested(Transfer transfer) {
        requested.add(transfer);
    }

    List<Transfer> requested() {
        return List.copyOf(requested);
    }
}
