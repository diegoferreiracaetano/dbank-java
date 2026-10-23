package com.diegoferreiracaetano.dbank.payments.application.service;

import com.diegoferreiracaetano.dbank.payments.application.port.in.ApplyTransferResultCommand;
import com.diegoferreiracaetano.dbank.payments.application.port.in.ApplyTransferResultUseCase;
import com.diegoferreiracaetano.dbank.payments.application.port.in.TransferResult;
import com.diegoferreiracaetano.dbank.payments.application.port.out.TransactionRunner;
import com.diegoferreiracaetano.dbank.payments.application.port.out.TransferRepository;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.Transfer;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferNotFoundException;

public class ApplyTransferResultService implements ApplyTransferResultUseCase {

    private final TransferRepository transfers;
    private final TransactionRunner transactionRunner;

    public ApplyTransferResultService(TransferRepository transfers, TransactionRunner transactionRunner) {
        this.transfers = transfers;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public void apply(ApplyTransferResultCommand command) {
        transactionRunner.run(() -> applyWithinTransaction(command));
    }

    private Transfer applyWithinTransaction(ApplyTransferResultCommand command) {
        var transfer = transfers
                .findById(command.transferId())
                .orElseThrow(() -> new TransferNotFoundException(command.transferId()));
        var updated = switch (command.result()) {
            case TransferResult.Settled() -> transfer.complete();
            case TransferResult.Rejected(var reason) -> transfer.fail(reason);
        };
        if (!updated.equals(transfer)) {
            transfers.save(updated);
        }
        return updated;
    }
}
