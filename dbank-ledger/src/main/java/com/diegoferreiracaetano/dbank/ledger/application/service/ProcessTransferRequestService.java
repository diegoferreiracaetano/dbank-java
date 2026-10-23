package com.diegoferreiracaetano.dbank.ledger.application.service;

import com.diegoferreiracaetano.dbank.ledger.application.port.in.PostTransferCommand;
import com.diegoferreiracaetano.dbank.ledger.application.port.in.ProcessTransferRequestUseCase;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.AccountRepository;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.LedgerTransactionRepository;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.TransactionRunner;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.TransferOutcomes;
import com.diegoferreiracaetano.dbank.ledger.application.transfer.RejectionReason;
import com.diegoferreiracaetano.dbank.ledger.application.transfer.TransferOutcome;
import com.diegoferreiracaetano.dbank.ledger.domain.account.Account;
import com.diegoferreiracaetano.dbank.ledger.domain.account.InsufficientFundsException;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransaction;
import java.time.Clock;

public class ProcessTransferRequestService implements ProcessTransferRequestUseCase {

    private final AccountRepository accounts;
    private final LedgerTransactionRepository transactions;
    private final TransferOutcomes outcomes;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public ProcessTransferRequestService(
            AccountRepository accounts,
            LedgerTransactionRepository transactions,
            TransferOutcomes outcomes,
            TransactionRunner transactionRunner,
            Clock clock) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.outcomes = outcomes;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    }

    @Override
    public TransferOutcome process(PostTransferCommand command) {
        return transactionRunner.call(() -> processWithinTransaction(command));
    }

    private TransferOutcome processWithinTransaction(PostTransferCommand command) {
        var known = outcomes.find(command.transactionId());
        if (known.isPresent()) {
            return known.get();
        }
        var outcome = attempt(command);
        outcomes.save(command.transactionId(), outcome, clock.instant());
        return outcome;
    }

    private TransferOutcome attempt(PostTransferCommand command) {
        var from = accounts.findById(command.from());
        var to = accounts.findById(command.to());
        if (from.isEmpty() || to.isEmpty()) {
            return new TransferOutcome.Rejected(RejectionReason.ACCOUNT_NOT_FOUND);
        }
        Account debited;
        try {
            debited = from.get().debit(command.amount());
        } catch (InsufficientFundsException e) {
            return new TransferOutcome.Rejected(RejectionReason.INSUFFICIENT_FUNDS);
        }
        var transaction = LedgerTransaction.transfer(
                command.transactionId(), command.from(), command.to(), command.amount(), clock.instant());
        accounts.save(debited);
        accounts.save(to.get().credit(command.amount()));
        transactions.save(transaction);
        return new TransferOutcome.Settled();
    }
}
