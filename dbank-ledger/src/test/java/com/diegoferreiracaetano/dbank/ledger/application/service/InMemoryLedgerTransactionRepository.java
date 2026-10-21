package com.diegoferreiracaetano.dbank.ledger.application.service;

import com.diegoferreiracaetano.dbank.ledger.application.port.out.LedgerTransactionRepository;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransaction;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransactionId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class InMemoryLedgerTransactionRepository implements LedgerTransactionRepository {

    private final Map<LedgerTransactionId, LedgerTransaction> transactions = new HashMap<>();

    @Override
    public boolean existsById(LedgerTransactionId id) {
        return transactions.containsKey(id);
    }

    @Override
    public void save(LedgerTransaction transaction) {
        transactions.put(transaction.id(), transaction);
    }

    List<LedgerTransaction> all() {
        return new ArrayList<>(transactions.values());
    }
}
