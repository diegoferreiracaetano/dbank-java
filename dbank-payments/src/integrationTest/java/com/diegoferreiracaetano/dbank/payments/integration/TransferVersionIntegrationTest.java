package com.diegoferreiracaetano.dbank.payments.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import org.springframework.dao.OptimisticLockingFailureException;

class TransferVersionIntegrationTest extends IntegrationTestBase {

    @Autowired
    TransferRepository transfers;

    @Test
    void givenTwoReadsOfTheSameTransfer_whenSavingBoth_thenTheSecondShouldBeRejected() {
        var transfer = pending();
        transfers.save(transfer);
        var first = transfers.findById(transfer.id()).orElseThrow();
        var second = transfers.findById(transfer.id()).orElseThrow();

        transfers.save(first.complete());

        assertThatThrownBy(() -> transfers.save(second.fail(FailureReason.INSUFFICIENT_FUNDS)))
                .isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(transfers.findById(transfer.id()).orElseThrow().status()).isEqualTo(TransferStatus.COMPLETED);
    }

    @Test
    void givenSavedChange_whenLoadingAgain_thenShouldHaveAHigherVersion() {
        var transfer = pending();
        transfers.save(transfer);
        var loaded = transfers.findById(transfer.id()).orElseThrow();

        transfers.save(loaded.complete());

        assertThat(transfers.findById(transfer.id()).orElseThrow().version()).isGreaterThan(loaded.version());
    }

    private Transfer pending() {
        return Transfer.request(
                TransferId.generate(),
                AccountId.generate(),
                AccountId.generate(),
                Money.ofCents(100),
                Instant.parse("2026-01-01T00:00:00Z"));
    }
}
