package com.diegoferreiracaetano.dbank.ledger.application.port.in;

import com.diegoferreiracaetano.dbank.ledger.domain.account.Account;

public interface OpenAccountUseCase {

    Account open();
}
