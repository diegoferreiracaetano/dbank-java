package com.diegoferreiracaetano.dbank.ledger.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.diegoferreiracaetano.dbank.ledger.application.port.out.AccountRepository;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.ConcurrentUpdateException;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.TransactionRunner;
import com.diegoferreiracaetano.dbank.ledger.domain.account.Account;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.shared.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AccountPersistenceIntegrationTest extends IntegrationTestBase {

    @Autowired
    AccountRepository accounts;

    @Autowired
    TransactionRunner runner;

    @Test
    void givenNewAccount_whenSavingAndLoading_thenShouldKeepBalanceAndStartAtVersionZero() {
        var id = AccountId.generate();
        accounts.save(new Account(id, Money.ofCents(1000)));

        var loaded = accounts.findById(id).orElseThrow();

        assertThat(loaded.balance()).isEqualTo(Money.ofCents(1000));
        assertThat(loaded.version()).isEqualTo(0L);
    }

    @Test
    void givenLoadedAccount_whenSavingAChange_thenShouldIncrementTheVersion() {
        var id = AccountId.generate();
        accounts.save(new Account(id, Money.ofCents(1000)));
        var loaded = accounts.findById(id).orElseThrow();

        accounts.save(loaded.debit(Money.ofCents(300)));

        var reloaded = accounts.findById(id).orElseThrow();
        assertThat(reloaded.balance()).isEqualTo(Money.ofCents(700));
        assertThat(reloaded.version()).isEqualTo(1L);
    }

    @Test
    void givenStaleAccount_whenSavingThroughTheRunner_thenShouldThrowConcurrentUpdateAndKeepTheFirstWrite() {
        var id = AccountId.generate();
        accounts.save(new Account(id, Money.ofCents(1000)));
        var first = accounts.findById(id).orElseThrow();
        var second = accounts.findById(id).orElseThrow();
        runner.run(() -> accounts.save(first.debit(Money.ofCents(300))));

        assertThatThrownBy(() -> runner.run(() -> accounts.save(second.debit(Money.ofCents(300)))))
                .isInstanceOf(ConcurrentUpdateException.class);

        var current = accounts.findById(id).orElseThrow();
        assertThat(current.balance()).isEqualTo(Money.ofCents(700));
        assertThat(current.version()).isEqualTo(1L);
    }

    @Test
    void givenUnknownId_whenFinding_thenShouldBeEmpty() {
        assertThat(accounts.findById(AccountId.generate())).isEmpty();
    }
}
