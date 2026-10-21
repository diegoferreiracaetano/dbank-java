package com.diegoferreiracaetano.dbank.ledger.application.port.in;

import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerEntry;
import java.util.List;

public interface GetStatementUseCase {

    List<LedgerEntry> get(AccountId id, int page, int size);
}
