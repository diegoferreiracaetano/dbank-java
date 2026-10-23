package com.diegoferreiracaetano.dbank.payments.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.diegoferreiracaetano.dbank.payments.application.port.in.ApplyTransferResultCommand;
import com.diegoferreiracaetano.dbank.payments.application.port.in.TransferResult;
import com.diegoferreiracaetano.dbank.payments.domain.shared.Money;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.AccountId;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.FailureReason;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.InvalidTransferTransitionException;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.Transfer;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferId;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferNotFoundException;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ApplyTransferResultServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final InMemoryTransferRepository transfers = new InMemoryTransferRepository();
    private final ApplyTransferResultService service =
            new ApplyTransferResultService(transfers, new DirectTransactionRunner());

    @Test
    void givenPendingTransfer_whenApplyingSettled_thenShouldComplete() {
        var transfer = pending();

        service.apply(settled(transfer));

        assertThat(statusOf(transfer)).isEqualTo(TransferStatus.COMPLETED);
    }

    @Test
    void givenPendingTransfer_whenApplyingRejected_thenShouldFailWithTheReason() {
        var transfer = pending();

        service.apply(rejected(transfer, FailureReason.INSUFFICIENT_FUNDS));

        var stored = transfers.findById(transfer.id()).orElseThrow();
        assertThat(stored.status()).isEqualTo(TransferStatus.FAILED);
        assertThat(stored.failureReason()).isEqualTo(FailureReason.INSUFFICIENT_FUNDS);
    }

    @Test
    void givenSameResultTwice_whenApplying_thenShouldWriteOnlyOnce() {
        var transfer = pending();
        service.apply(settled(transfer));

        service.apply(settled(transfer));

        assertThat(transfers.saves()).isEqualTo(2);
        assertThat(statusOf(transfer)).isEqualTo(TransferStatus.COMPLETED);
    }

    @Test
    void givenCompletedTransfer_whenApplyingRejected_thenShouldThrowAndKeepTheState() {
        var transfer = pending();
        service.apply(settled(transfer));

        assertThatThrownBy(() -> service.apply(rejected(transfer, FailureReason.INSUFFICIENT_FUNDS)))
                .isInstanceOf(InvalidTransferTransitionException.class);

        assertThat(statusOf(transfer)).isEqualTo(TransferStatus.COMPLETED);
    }

    @Test
    void givenFailedTransfer_whenApplyingSettled_thenShouldThrowAndKeepTheState() {
        var transfer = pending();
        service.apply(rejected(transfer, FailureReason.ACCOUNT_NOT_FOUND));

        assertThatThrownBy(() -> service.apply(settled(transfer)))
                .isInstanceOf(InvalidTransferTransitionException.class);

        assertThat(statusOf(transfer)).isEqualTo(TransferStatus.FAILED);
    }

    @Test
    void givenUnknownTransfer_whenApplying_thenShouldThrowNotFound() {
        var command = new ApplyTransferResultCommand(TransferId.generate(), new TransferResult.Settled());

        assertThatThrownBy(() -> service.apply(command)).isInstanceOf(TransferNotFoundException.class);
    }

    private Transfer pending() {
        var transfer = Transfer.request(
                TransferId.generate(), AccountId.generate(), AccountId.generate(), Money.ofCents(100), NOW);
        transfers.save(transfer);
        return transfer;
    }

    private TransferStatus statusOf(Transfer transfer) {
        return transfers.findById(transfer.id()).orElseThrow().status();
    }

    private ApplyTransferResultCommand settled(Transfer transfer) {
        return new ApplyTransferResultCommand(transfer.id(), new TransferResult.Settled());
    }

    private ApplyTransferResultCommand rejected(Transfer transfer, FailureReason reason) {
        return new ApplyTransferResultCommand(transfer.id(), new TransferResult.Rejected(reason));
    }
}
