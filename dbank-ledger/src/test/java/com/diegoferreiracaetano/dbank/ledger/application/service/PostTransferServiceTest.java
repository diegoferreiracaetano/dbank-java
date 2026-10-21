package com.diegoferreiracaetano.dbank.ledger.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.diegoferreiracaetano.dbank.ledger.application.port.in.PostTransferCommand;
import com.diegoferreiracaetano.dbank.ledger.domain.account.Account;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountNotFoundException;
import com.diegoferreiracaetano.dbank.ledger.domain.account.InsufficientFundsException;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransactionId;
import com.diegoferreiracaetano.dbank.ledger.domain.shared.Money;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PostTransferServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final InMemoryLedgerTransactionRepository transactions = new InMemoryLedgerTransactionRepository();
    private final PostTransferService service =
            new PostTransferService(accounts, transactions, new DirectTransactionRunner(), CLOCK);

    private final AccountId fromId = AccountId.generate();
    private final AccountId toId = AccountId.generate();

    @Test
    void givenFundedAccounts_whenPosting_thenShouldMoveMoneyAndStoreTransaction() {
        accounts.save(new Account(fromId, Money.ofCents(1000)));
        accounts.save(new Account(toId, Money.ofCents(200)));
        var command = new PostTransferCommand(LedgerTransactionId.generate(), fromId, toId, Money.ofCents(300));

        service.post(command);

        assertThat(accounts.findById(fromId).orElseThrow().balance()).isEqualTo(Money.ofCents(700));
        assertThat(accounts.findById(toId).orElseThrow().balance()).isEqualTo(Money.ofCents(500));
        assertThat(transactions.all()).hasSize(1);
    }

    @Test
    void givenInsufficientBalance_whenPosting_thenShouldRejectAndStoreNothing() {
        accounts.save(new Account(fromId, Money.ofCents(100)));
        accounts.save(new Account(toId, Money.ofCents(200)));
        var command = new PostTransferCommand(LedgerTransactionId.generate(), fromId, toId, Money.ofCents(300));

        assertThatThrownBy(() -> service.post(command)).isInstanceOf(InsufficientFundsException.class);

        assertThat(accounts.findById(fromId).orElseThrow().balance()).isEqualTo(Money.ofCents(100));
        assertThat(accounts.findById(toId).orElseThrow().balance()).isEqualTo(Money.ofCents(200));
        assertThat(transactions.all()).isEmpty();
    }

    @Test
    void givenUnknownSourceAccount_whenPosting_thenShouldThrowAccountNotFound() {
        accounts.save(new Account(toId, Money.ofCents(200)));
        var command = new PostTransferCommand(LedgerTransactionId.generate(), fromId, toId, Money.ofCents(300));

        assertThatThrownBy(() -> service.post(command)).isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void givenUnknownDestinationAccount_whenPosting_thenShouldThrowAccountNotFound() {
        accounts.save(new Account(fromId, Money.ofCents(1000)));
        var command = new PostTransferCommand(LedgerTransactionId.generate(), fromId, toId, Money.ofCents(300));

        assertThatThrownBy(() -> service.post(command)).isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void givenAlreadyPostedTransaction_whenPostingAgain_thenShouldNotMoveMoneyTwice() {
        accounts.save(new Account(fromId, Money.ofCents(1000)));
        accounts.save(new Account(toId, Money.ofCents(200)));
        var command = new PostTransferCommand(LedgerTransactionId.generate(), fromId, toId, Money.ofCents(300));
        service.post(command);

        service.post(command);

        assertThat(accounts.findById(fromId).orElseThrow().balance()).isEqualTo(Money.ofCents(700));
        assertThat(accounts.findById(toId).orElseThrow().balance()).isEqualTo(Money.ofCents(500));
        assertThat(transactions.all()).hasSize(1);
    }
}
