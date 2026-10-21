package com.diegoferreiracaetano.dbank.ledger.application.port.out;

import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransaction;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransactionId;

public interface LedgerTransactionRepository {

    boolean existsById(LedgerTransactionId id);

    void save(LedgerTransaction transaction);
}
