package com.diegoferreiracaetano.dbank.payments.application.service;

import com.diegoferreiracaetano.dbank.payments.application.port.out.TransferRepository;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.Transfer;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class InMemoryTransferRepository implements TransferRepository {

    private final Map<TransferId, Transfer> transfers = new HashMap<>();

    @Override
    public void save(Transfer transfer) {
        transfers.put(transfer.id(), transfer);
    }

    @Override
    public Optional<Transfer> findById(TransferId id) {
        return Optional.ofNullable(transfers.get(id));
    }

    List<Transfer> all() {
        return List.copyOf(transfers.values());
    }
}
