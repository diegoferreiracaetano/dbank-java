package com.diegoferreiracaetano.dbank.ledger.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.diegoferreiracaetano.dbank.ledger.domain.account.Account;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountNotFoundException;
import com.diegoferreiracaetano.dbank.ledger.domain.shared.Money;
import org.junit.jupiter.api.Test;

class GetAccountServiceTest {

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final GetAccountService service = new GetAccountService(accounts);

    @Test
    void givenExistingAccount_whenGetting_thenShouldReturnIt() {
        var account = new Account(AccountId.generate(), Money.ofCents(250));
        accounts.save(account);

        var result = service.get(account.id());

        assertThat(result).isEqualTo(account);
    }

    @Test
    void givenUnknownAccount_whenGetting_thenShouldThrowAccountNotFound() {
        var id = AccountId.generate();

        assertThatThrownBy(() -> service.get(id)).isInstanceOf(AccountNotFoundException.class);
    }
}
