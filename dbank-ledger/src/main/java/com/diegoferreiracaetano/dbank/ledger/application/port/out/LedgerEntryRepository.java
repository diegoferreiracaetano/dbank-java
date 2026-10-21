package com.diegoferreiracaetano.dbank.ledger.application.port.out;

import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerEntry;
import java.util.List;

public interface LedgerEntryRepository {

    List<LedgerEntry> findByAccountId(AccountId id, int page, int size);
}
