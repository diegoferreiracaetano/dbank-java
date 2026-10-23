package com.diegoferreiracaetano.dbank.ledger.application.service;

import com.diegoferreiracaetano.dbank.ledger.application.port.out.TransferResultEvents;
import com.diegoferreiracaetano.dbank.ledger.application.transfer.TransferOutcome;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransactionId;
import java.util.ArrayList;
import java.util.List;

class InMemoryTransferResultEvents implements TransferResultEvents {

    record RecordedEvent(LedgerTransactionId transferId, AccountId from, TransferOutcome outcome) {}

    private final List<RecordedEvent> recorded = new ArrayList<>();

    @Override
    public void transferProcessed(LedgerTransactionId transferId, AccountId from, TransferOutcome outcome) {
        recorded.add(new RecordedEvent(transferId, from, outcome));
    }

    List<RecordedEvent> recorded() {
        return List.copyOf(recorded);
    }
}
