package com.diegoferreiracaetano.dbank.payments.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegoferreiracaetano.dbank.payments.application.port.out.TransferRepository;
import com.diegoferreiracaetano.dbank.payments.domain.shared.Money;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.AccountId;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.FailureReason;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.Transfer;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferId;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TransferPersistenceIntegrationTest extends IntegrationTestBase {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    TransferRepository transfers;

    @Test
    void givenNewTransfer_whenSavingAndLoading_thenShouldReturnEqualTransfer() {
        var transfer = pending();
        transfers.save(transfer);

        var loaded = transfers.findById(transfer.id());

        assertThat(loaded).contains(transfer);
    }

    @Test
    void givenSavedTransfer_whenCompletingAndSavingAgain_thenShouldPersistTheNewStatus() {
        var transfer = pending();
        transfers.save(transfer);

        transfers.save(transfer.complete());

        assertThat(transfers.findById(transfer.id()).orElseThrow().status()).isEqualTo(TransferStatus.COMPLETED);
    }

    @Test
    void givenFailedTransfer_whenSavingAndLoading_thenShouldKeepTheFailureReason() {
        var transfer = pending().fail(FailureReason.ACCOUNT_NOT_FOUND);
        transfers.save(transfer);

        var loaded = transfers.findById(transfer.id()).orElseThrow();

        assertThat(loaded.failureReason()).isEqualTo(FailureReason.ACCOUNT_NOT_FOUND);
    }

    @Test
    void givenUnknownId_whenFinding_thenShouldBeEmpty() {
        assertThat(transfers.findById(TransferId.generate())).isEmpty();
    }

    private Transfer pending() {
        return Transfer.request(
                TransferId.generate(), AccountId.generate(), AccountId.generate(), Money.ofCents(500), NOW);
    }
}
