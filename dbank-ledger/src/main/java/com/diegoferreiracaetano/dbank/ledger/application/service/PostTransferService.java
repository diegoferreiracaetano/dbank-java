package com.diegoferreiracaetano.dbank.ledger.application.service;

import com.diegoferreiracaetano.dbank.ledger.application.port.in.PostTransferCommand;
import com.diegoferreiracaetano.dbank.ledger.application.port.in.PostTransferUseCase;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.AccountRepository;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.LedgerTransactionRepository;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.TransactionRunner;
import com.diegoferreiracaetano.dbank.ledger.domain.account.Account;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountNotFoundException;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransaction;
import java.time.Clock;

public class PostTransferService implements PostTransferUseCase {

    private final AccountRepository accounts;
    private final LedgerTransactionRepository transactions;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public PostTransferService(
            AccountRepository accounts,
            LedgerTransactionRepository transactions,
            TransactionRunner transactionRunner,
            Clock clock) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    }

    @Override
    public void post(PostTransferCommand command) {
        transactionRunner.run(() -> postWithinTransaction(command));
    }

    private void postWithinTransaction(PostTransferCommand command) {
        if (transactions.existsById(command.transactionId())) {
            return;
        }
        var from = load(command.from());
        var to = load(command.to());
        var transaction = LedgerTransaction.transfer(
                command.transactionId(), command.from(), command.to(), command.amount(), clock.instant());

        accounts.save(from.debit(command.amount()));
        accounts.save(to.credit(command.amount()));
        transactions.save(transaction);
    }

    private Account load(AccountId id) {
        return accounts.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
    }
}
