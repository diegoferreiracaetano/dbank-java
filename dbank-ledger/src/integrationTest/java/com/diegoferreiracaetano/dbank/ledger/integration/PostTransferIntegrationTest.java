package com.diegoferreiracaetano.dbank.ledger.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.diegoferreiracaetano.dbank.ledger.application.port.in.PostTransferCommand;
import com.diegoferreiracaetano.dbank.ledger.application.port.in.PostTransferUseCase;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.AccountRepository;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.LedgerTransactionRepository;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.TransactionRunner;
import com.diegoferreiracaetano.dbank.ledger.domain.account.Account;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.account.InsufficientFundsException;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransaction;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransactionId;
import com.diegoferreiracaetano.dbank.ledger.domain.shared.Money;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class PostTransferIntegrationTest extends IntegrationTestBase {

    @Autowired
    PostTransferUseCase postTransfer;

    @Autowired
    AccountRepository accounts;

    @Autowired
    LedgerTransactionRepository transactions;

    @Autowired
    TransactionRunner runner;

    @Autowired
    JdbcTemplate jdbc;

    private final AccountId from = AccountId.generate();
    private final AccountId to = AccountId.generate();

    @Test
    void givenFundedAccounts_whenPosting_thenShouldPersistBalancesAndEntries() {
        fund(1000, 200);
        var id = LedgerTransactionId.generate();

        postTransfer.post(new PostTransferCommand(id, from, to, Money.ofCents(300)));

        assertThat(balanceOf(from)).isEqualTo(Money.ofCents(700));
        assertThat(balanceOf(to)).isEqualTo(Money.ofCents(500));
        assertThat(transactions.existsById(id)).isTrue();
        assertThat(entryCount(id)).isEqualTo(2);
    }

    @Test
    void givenAlreadyPostedTransaction_whenPostingAgain_thenShouldNotChangeBalancesOrEntries() {
        fund(1000, 200);
        var command = new PostTransferCommand(LedgerTransactionId.generate(), from, to, Money.ofCents(300));
        postTransfer.post(command);

        postTransfer.post(command);

        assertThat(balanceOf(from)).isEqualTo(Money.ofCents(700));
        assertThat(balanceOf(to)).isEqualTo(Money.ofCents(500));
        assertThat(entryCount(command.transactionId())).isEqualTo(2);
    }

    @Test
    void givenInsufficientFunds_whenPosting_thenShouldStoreNothing() {
        fund(100, 200);
        var id = LedgerTransactionId.generate();

        assertThatThrownBy(() -> postTransfer.post(new PostTransferCommand(id, from, to, Money.ofCents(300))))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(balanceOf(from)).isEqualTo(Money.ofCents(100));
        assertThat(balanceOf(to)).isEqualTo(Money.ofCents(200));
        assertThat(transactions.existsById(id)).isFalse();
    }

    @Test
    void givenFailureAfterSavingAccounts_whenRunningInTransaction_thenShouldRollbackTheAccountChanges() {
        fund(1000, 200);
        var source = accounts.findById(from).orElseThrow();

        assertThatThrownBy(() -> runner.run(() -> {
                    accounts.save(source.debit(Money.ofCents(300)));
                    throw new IllegalStateException("boom");
                }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(balanceOf(from)).isEqualTo(Money.ofCents(1000));
        assertThat(accounts.findById(from).orElseThrow().version()).isEqualTo(0L);
    }

    @Test
    void givenFailureAfterSavingTheTransaction_whenRunningInTransaction_thenShouldRollbackEverything() {
        fund(1000, 200);
        var id = LedgerTransactionId.generate();
        var source = accounts.findById(from).orElseThrow();
        var target = accounts.findById(to).orElseThrow();

        assertThatThrownBy(() -> runner.run(() -> {
                    accounts.save(source.debit(Money.ofCents(300)));
                    accounts.save(target.credit(Money.ofCents(300)));
                    transactions.save(LedgerTransaction.transfer(id, from, to, Money.ofCents(300), Instant.now()));
                    throw new IllegalStateException("boom");
                }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(balanceOf(from)).isEqualTo(Money.ofCents(1000));
        assertThat(balanceOf(to)).isEqualTo(Money.ofCents(200));
        assertThat(transactions.existsById(id)).isFalse();
        assertThat(entryCount(id)).isZero();
    }

    private void fund(long fromCents, long toCents) {
        accounts.save(new Account(from, Money.ofCents(fromCents)));
        accounts.save(new Account(to, Money.ofCents(toCents)));
    }

    private Money balanceOf(AccountId id) {
        return accounts.findById(id).orElseThrow().balance();
    }

    private Integer entryCount(LedgerTransactionId id) {
        return jdbc.queryForObject(
                "select count(*) from ledger_entry where transaction_id = ?", Integer.class, id.value());
    }
}
