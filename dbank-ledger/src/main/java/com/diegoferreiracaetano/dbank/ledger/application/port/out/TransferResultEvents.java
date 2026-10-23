package com.diegoferreiracaetano.dbank.ledger.application.port.out;

import com.diegoferreiracaetano.dbank.ledger.application.transfer.TransferOutcome;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransactionId;

public interface TransferResultEvents {

    void transferProcessed(LedgerTransactionId transferId, AccountId from, TransferOutcome outcome);
}
