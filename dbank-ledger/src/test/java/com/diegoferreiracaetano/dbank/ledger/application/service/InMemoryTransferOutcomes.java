package com.diegoferreiracaetano.dbank.ledger.application.service;

import com.diegoferreiracaetano.dbank.ledger.application.port.out.TransferOutcomes;
import com.diegoferreiracaetano.dbank.ledger.application.transfer.TransferOutcome;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransactionId;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class InMemoryTransferOutcomes implements TransferOutcomes {

    private final Map<LedgerTransactionId, TransferOutcome> outcomes = new HashMap<>();

    @Override
    public Optional<TransferOutcome> find(LedgerTransactionId transferId) {
        return Optional.ofNullable(outcomes.get(transferId));
    }

    @Override
    public void save(LedgerTransactionId transferId, TransferOutcome outcome, Instant processedAt) {
        outcomes.put(transferId, outcome);
    }

    int size() {
        return outcomes.size();
    }
}
