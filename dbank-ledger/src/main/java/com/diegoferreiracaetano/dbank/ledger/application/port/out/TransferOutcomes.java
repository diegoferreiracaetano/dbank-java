package com.diegoferreiracaetano.dbank.ledger.application.port.out;

import com.diegoferreiracaetano.dbank.ledger.application.transfer.TransferOutcome;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransactionId;
import java.time.Instant;
import java.util.Optional;

public interface TransferOutcomes {

    Optional<TransferOutcome> find(LedgerTransactionId transferId);

    void save(LedgerTransactionId transferId, TransferOutcome outcome, Instant processedAt);
}
