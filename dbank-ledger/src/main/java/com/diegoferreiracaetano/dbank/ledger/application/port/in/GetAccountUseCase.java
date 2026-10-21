package com.diegoferreiracaetano.dbank.ledger.application.port.in;

import com.diegoferreiracaetano.dbank.ledger.domain.account.Account;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;

public interface GetAccountUseCase {

    Account get(AccountId id);
}
