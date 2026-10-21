package com.diegoferreiracaetano.dbank.ledger.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegoferreiracaetano.dbank.ledger.domain.shared.Money;
import org.junit.jupiter.api.Test;

class OpenAccountServiceTest {

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final OpenAccountService service = new OpenAccountService(accounts);

    @Test
    void givenNoAccount_whenOpening_thenShouldReturnAccountWithZeroBalance() {
        var account = service.open();

        assertThat(account.balance()).isEqualTo(Money.ZERO);
    }

    @Test
    void givenNoAccount_whenOpening_thenShouldPersistTheAccount() {
        var account = service.open();

        assertThat(accounts.findById(account.id())).contains(account);
    }
}
