package com.diegoferreiracaetano.dbank.notifications.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationMessagesTest {

    private final UUID transferId = UUID.randomUUID();

    @Test
    void givenSettledEvent_whenComposing_thenShouldSayTheTransferWasCompleted() {
        var event = event(TransferResultEvent.Outcome.SETTLED, null);

        assertThat(NotificationMessages.of(event)).isEqualTo("Your transfer " + transferId + " was completed.");
    }

    @Test
    void givenInsufficientFunds_whenComposing_thenShouldExplainIt() {
        var event = event(TransferResultEvent.Outcome.REJECTED, "INSUFFICIENT_FUNDS");

        assertThat(NotificationMessages.of(event))
                .isEqualTo("Your transfer " + transferId + " was declined: insufficient funds.");
    }

    @Test
    void givenAccountNotFound_whenComposing_thenShouldExplainIt() {
        var event = event(TransferResultEvent.Outcome.REJECTED, "ACCOUNT_NOT_FOUND");

        assertThat(NotificationMessages.of(event))
                .isEqualTo("Your transfer " + transferId + " was declined: account not found.");
    }

    @Test
    void givenUnknownReason_whenComposing_thenShouldUseAGenericExplanation() {
        var event = event(TransferResultEvent.Outcome.REJECTED, "SOMETHING_NEW");

        assertThat(NotificationMessages.of(event))
                .isEqualTo("Your transfer " + transferId + " was declined: it could not be processed.");
    }

    @Test
    void givenRejectionWithoutReason_whenComposing_thenShouldUseAGenericExplanation() {
        var event = event(TransferResultEvent.Outcome.REJECTED, null);

        assertThat(NotificationMessages.of(event)).endsWith("declined: it could not be processed.");
    }

    private TransferResultEvent event(TransferResultEvent.Outcome outcome, String reason) {
        return new TransferResultEvent(UUID.randomUUID(), outcome, transferId, UUID.randomUUID(), reason);
    }
}
