package com.diegoferreiracaetano.dbank.payments.application.service;

import com.diegoferreiracaetano.dbank.payments.application.idempotency.ClaimResult;
import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyKeyReuseException;
import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyRecord;
import com.diegoferreiracaetano.dbank.payments.application.idempotency.RequestFingerprint;
import com.diegoferreiracaetano.dbank.payments.application.port.in.RequestTransferCommand;
import com.diegoferreiracaetano.dbank.payments.application.port.in.RequestTransferResult;
import com.diegoferreiracaetano.dbank.payments.application.port.in.RequestTransferUseCase;
import com.diegoferreiracaetano.dbank.payments.application.port.out.IdempotencyStore;
import com.diegoferreiracaetano.dbank.payments.application.port.out.TransactionRunner;
import com.diegoferreiracaetano.dbank.payments.application.port.out.TransferEvents;
import com.diegoferreiracaetano.dbank.payments.application.port.out.TransferRepository;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.Transfer;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferId;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferNotFoundException;
import java.time.Clock;

public class RequestTransferService implements RequestTransferUseCase {

    private final TransferRepository transfers;
    private final IdempotencyStore idempotency;
    private final TransferEvents events;
    private final TransactionRunner transactionRunner;
    private final Clock clock;

    public RequestTransferService(
            TransferRepository transfers,
            IdempotencyStore idempotency,
            TransferEvents events,
            TransactionRunner transactionRunner,
            Clock clock) {
        this.transfers = transfers;
        this.idempotency = idempotency;
        this.events = events;
        this.transactionRunner = transactionRunner;
        this.clock = clock;
    }

    @Override
    public RequestTransferResult request(RequestTransferCommand command) {
        var candidate = Transfer.request(
                TransferId.generate(), command.from(), command.to(), command.amount(), clock.instant());
        var fingerprint = RequestFingerprint.of(command.from(), command.to(), command.amount());
        var record = new IdempotencyRecord(command.idempotencyKey(), fingerprint, candidate.id());
        return transactionRunner.run(() -> claimAndSave(candidate, record));
    }

    private RequestTransferResult claimAndSave(Transfer candidate, IdempotencyRecord record) {
        return switch (idempotency.claim(record)) {
            case ClaimResult.Claimed() -> {
                transfers.save(candidate);
                events.transferRequested(candidate);
                yield new RequestTransferResult(candidate, false);
            }
            case ClaimResult.Existing(var existing) -> replay(existing, record.fingerprint());
        };
    }

    private RequestTransferResult replay(IdempotencyRecord existing, RequestFingerprint fingerprint) {
        if (!existing.fingerprint().equals(fingerprint)) {
            throw new IdempotencyKeyReuseException(existing.key());
        }
        var transfer = transfers
                .findById(existing.transferId())
                .orElseThrow(() -> new TransferNotFoundException(existing.transferId()));
        return new RequestTransferResult(transfer, true);
    }
}
