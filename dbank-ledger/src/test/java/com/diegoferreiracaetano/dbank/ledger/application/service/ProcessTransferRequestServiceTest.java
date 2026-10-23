package com.diegoferreiracaetano.dbank.ledger.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegoferreiracaetano.dbank.ledger.application.port.in.PostTransferCommand;
import com.diegoferreiracaetano.dbank.ledger.application.service.InMemoryTransferResultEvents.RecordedEvent;
import com.diegoferreiracaetano.dbank.ledger.application.transfer.RejectionReason;
import com.diegoferreiracaetano.dbank.ledger.application.transfer.TransferOutcome;
import com.diegoferreiracaetano.dbank.ledger.domain.account.Account;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransactionId;
import com.diegoferreiracaetano.dbank.ledger.domain.shared.Money;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class ProcessTransferRequestServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final InMemoryLedgerTransactionRepository transactions = new InMemoryLedgerTransactionRepository();
    private final InMemoryTransferOutcomes outcomes = new InMemoryTransferOutcomes();
    private final InMemoryTransferResultEvents events = new InMemoryTransferResultEvents();
    private final ProcessTransferRequestService service = new ProcessTransferRequestService(
            accounts, transactions, outcomes, events, new DirectTransactionRunner(), CLOCK);

    private final AccountId from = AccountId.generate();
    private final AccountId to = AccountId.generate();

    @Test
    void givenFundedAccounts_whenProcessing_thenShouldSettleAndMoveTheBalances() {
        fund(1_000, 0);

        var outcome = service.process(command(to, 100));

        assertThat(outcome).isEqualTo(new TransferOutcome.Settled());
        assertThat(balanceOf(from)).isEqualTo(Money.ofCents(900));
        assertThat(balanceOf(to)).isEqualTo(Money.ofCents(100));
        assertThat(transactions.all()).hasSize(1);
    }

    @Test
    void givenSameTransferTwice_whenProcessing_thenShouldMoveTheMoneyOnlyOnce() {
        fund(1_000, 0);
        var command = command(to, 100);
        service.process(command);

        var second = service.process(command);

        assertThat(second).isEqualTo(new TransferOutcome.Settled());
        assertThat(balanceOf(from)).isEqualTo(Money.ofCents(900));
        assertThat(transactions.all()).hasSize(1);
    }

    @Test
    void givenInsufficientFunds_whenProcessing_thenShouldRejectAndLeaveTheBalances() {
        fund(1_000, 0);

        var outcome = service.process(command(to, 5_000));

        assertThat(outcome).isEqualTo(new TransferOutcome.Rejected(RejectionReason.INSUFFICIENT_FUNDS));
        assertThat(balanceOf(from)).isEqualTo(Money.ofCents(1_000));
        assertThat(balanceOf(to)).isEqualTo(Money.ZERO);
        assertThat(transactions.all()).isEmpty();
        assertThat(outcomes.size()).isEqualTo(1);
    }

    @Test
    void givenUnknownDestination_whenProcessing_thenShouldRejectAsAccountNotFound() {
        fund(1_000, 0);

        var outcome = service.process(command(AccountId.generate(), 100));

        assertThat(outcome).isEqualTo(new TransferOutcome.Rejected(RejectionReason.ACCOUNT_NOT_FOUND));
        assertThat(balanceOf(from)).isEqualTo(Money.ofCents(1_000));
    }

    @Test
    void givenUnknownSource_whenProcessing_thenShouldRejectAsAccountNotFound() {
        accounts.save(new Account(to, Money.ZERO));

        var outcome = service.process(command(to, 100));

        assertThat(outcome).isEqualTo(new TransferOutcome.Rejected(RejectionReason.ACCOUNT_NOT_FOUND));
    }

    @Test
    void givenRejectedTransfer_whenTheAccountGetsFundedAndItIsProcessedAgain_thenShouldKeepTheRejection() {
        fund(1_000, 0);
        var command = command(to, 5_000);
        service.process(command);
        accounts.save(new Account(from, Money.ofCents(10_000)));

        var again = service.process(command);

        assertThat(again).isEqualTo(new TransferOutcome.Rejected(RejectionReason.INSUFFICIENT_FUNDS));
        assertThat(balanceOf(from)).isEqualTo(Money.ofCents(10_000));
        assertThat(transactions.all()).isEmpty();
    }

    @Test
    void givenSettledTransfer_whenProcessing_thenShouldRecordOneEventWithTheOutcome() {
        fund(1_000, 0);
        var command = command(to, 100);

        service.process(command);

        assertThat(events.recorded())
                .containsExactly(new RecordedEvent(command.transactionId(), from, new TransferOutcome.Settled()));
    }

    @Test
    void givenRejectedTransfer_whenProcessing_thenShouldRecordOneEventWithTheReason() {
        fund(1_000, 0);

        service.process(command(to, 5_000));

        assertThat(events.recorded()).hasSize(1);
        assertThat(events.recorded().get(0).outcome())
                .isEqualTo(new TransferOutcome.Rejected(RejectionReason.INSUFFICIENT_FUNDS));
    }

    @Test
    void givenSameTransferTwice_whenProcessing_thenShouldRecordOnlyOneEvent() {
        fund(1_000, 0);
        var command = command(to, 100);
        service.process(command);

        service.process(command);

        assertThat(events.recorded()).hasSize(1);
    }

    private void fund(long fromCents, long toCents) {
        accounts.save(new Account(from, Money.ofCents(fromCents)));
        accounts.save(new Account(to, Money.ofCents(toCents)));
    }

    private Money balanceOf(AccountId id) {
        return accounts.findById(id).orElseThrow().balance();
    }

    private PostTransferCommand command(AccountId destination, long cents) {
        return new PostTransferCommand(LedgerTransactionId.generate(), from, destination, Money.ofCents(cents));
    }
}
