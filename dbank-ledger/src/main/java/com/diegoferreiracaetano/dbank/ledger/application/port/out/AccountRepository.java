package com.diegoferreiracaetano.dbank.ledger.application.port.out;

import com.diegoferreiracaetano.dbank.ledger.domain.account.Account;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import java.util.Optional;

public interface AccountRepository {

    Optional<Account> findById(AccountId id);

    void save(Account account);
}
