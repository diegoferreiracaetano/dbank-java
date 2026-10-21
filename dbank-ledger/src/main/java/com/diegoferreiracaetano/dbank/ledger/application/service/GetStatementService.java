package com.diegoferreiracaetano.dbank.ledger.application.service;

import com.diegoferreiracaetano.dbank.ledger.application.port.in.GetStatementUseCase;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.AccountRepository;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.LedgerEntryRepository;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountNotFoundException;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerEntry;
import java.util.List;

public class GetStatementService implements GetStatementUseCase {

    private final AccountRepository accounts;
    private final LedgerEntryRepository entries;

    public GetStatementService(AccountRepository accounts, LedgerEntryRepository entries) {
        this.accounts = accounts;
        this.entries = entries;
    }

    @Override
    public List<LedgerEntry> get(AccountId id, int page, int size) {
        accounts.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
        return entries.findByAccountId(id, page, size);
    }
}
