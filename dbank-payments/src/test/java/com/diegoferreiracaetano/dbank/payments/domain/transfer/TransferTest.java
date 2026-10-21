package com.diegoferreiracaetano.dbank.payments.domain.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.diegoferreiracaetano.dbank.payments.domain.shared.Money;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TransferTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final AccountId from = AccountId.generate();
    private final AccountId to = AccountId.generate();

    @Test
    void givenValidData_whenRequesting_thenShouldBePendingWithoutFailureReason() {
        var transfer = pending();

        assertThat(transfer.status()).isEqualTo(TransferStatus.PENDING);
        assertThat(transfer.failureReason()).isNull();
        assertThat(transfer.createdAt()).isEqualTo(NOW);
    }

    @Test
    void givenSameAccountOnBothSides_whenRequesting_thenShouldReject() {
        assertThatThrownBy(() -> Transfer.request(TransferId.generate(), from, from, Money.ofCents(100), NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenZeroAmount_whenRequesting_thenShouldReject() {
        assertThatThrownBy(() -> Transfer.request(TransferId.generate(), from, to, Money.ZERO, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenFailedStatusWithoutReason_whenCreating_thenShouldReject() {
        assertThatThrownBy(() -> new Transfer(
                        TransferId.generate(), from, to, Money.ofCents(100), TransferStatus.FAILED, null, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenPendingStatusWithReason_whenCreating_thenShouldReject() {
        assertThatThrownBy(() -> new Transfer(
                        TransferId.generate(),
                        from,
                        to,
                        Money.ofCents(100),
                        TransferStatus.PENDING,
                        FailureReason.INSUFFICIENT_FUNDS,
                        NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenPendingTransfer_whenCompleting_thenShouldBeCompleted() {
        var transfer = pending().complete();

        assertThat(transfer.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(transfer.failureReason()).isNull();
    }

    @Test
    void givenPendingTransfer_whenFailing_thenShouldBeFailedWithTheReason() {
        var transfer = pending().fail(FailureReason.INSUFFICIENT_FUNDS);

        assertThat(transfer.status()).isEqualTo(TransferStatus.FAILED);
        assertThat(transfer.failureReason()).isEqualTo(FailureReason.INSUFFICIENT_FUNDS);
    }

    @Test
    void givenCompletedTransfer_whenCompletingAgain_thenShouldStayTheSame() {
        var completed = pending().complete();

        var result = completed.complete();

        assertThat(result).isEqualTo(completed);
    }

    @Test
    void givenFailedTransfer_whenFailingWithTheSameReason_thenShouldStayTheSame() {
        var failed = pending().fail(FailureReason.ACCOUNT_NOT_FOUND);

        var result = failed.fail(FailureReason.ACCOUNT_NOT_FOUND);

        assertThat(result).isEqualTo(failed);
    }

    @Test
    void givenCompletedTransfer_whenFailing_thenShouldRejectAsContradiction() {
        var completed = pending().complete();

        assertThatThrownBy(() -> completed.fail(FailureReason.INSUFFICIENT_FUNDS))
                .isInstanceOf(InvalidTransferTransitionException.class);
    }

    @Test
    void givenFailedTransfer_whenCompleting_thenShouldRejectAsContradiction() {
        var failed = pending().fail(FailureReason.INSUFFICIENT_FUNDS);

        assertThatThrownBy(failed::complete).isInstanceOf(InvalidTransferTransitionException.class);
    }

    @Test
    void givenFailedTransfer_whenFailingWithADifferentReason_thenShouldRejectAsContradiction() {
        var failed = pending().fail(FailureReason.INSUFFICIENT_FUNDS);

        assertThatThrownBy(() -> failed.fail(FailureReason.ACCOUNT_NOT_FOUND))
                .isInstanceOf(InvalidTransferTransitionException.class);
    }

    @Test
    void givenPendingTransfer_whenCompleting_thenOriginalShouldNotChange() {
        var original = pending();

        original.complete();

        assertThat(original.status()).isEqualTo(TransferStatus.PENDING);
    }

    @Test
    void givenTransition_whenCompleting_thenShouldKeepIdentityAccountsAmountAndCreationTime() {
        var original = pending();

        var completed = original.complete();

        assertThat(completed.id()).isEqualTo(original.id());
        assertThat(completed.from()).isEqualTo(from);
        assertThat(completed.to()).isEqualTo(to);
        assertThat(completed.amount()).isEqualTo(Money.ofCents(100));
        assertThat(completed.createdAt()).isEqualTo(NOW);
    }

    private Transfer pending() {
        return Transfer.request(TransferId.generate(), from, to, Money.ofCents(100), NOW);
    }
}
