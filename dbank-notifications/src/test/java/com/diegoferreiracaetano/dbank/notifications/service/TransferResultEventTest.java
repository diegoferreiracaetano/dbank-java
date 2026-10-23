package com.diegoferreiracaetano.dbank.notifications.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TransferResultEventTest {

    @Test
    void givenMissingEventId_whenCreating_thenShouldReject() {
        assertThatThrownBy(() -> new TransferResultEvent(
                        null, TransferResultEvent.Outcome.SETTLED, UUID.randomUUID(), UUID.randomUUID(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenMissingTransferId_whenCreating_thenShouldReject() {
        assertThatThrownBy(() -> new TransferResultEvent(
                        UUID.randomUUID(), TransferResultEvent.Outcome.SETTLED, null, UUID.randomUUID(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenMissingOutcome_whenCreating_thenShouldReject() {
        assertThatThrownBy(() ->
                        new TransferResultEvent(UUID.randomUUID(), null, UUID.randomUUID(), UUID.randomUUID(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenMissingAccountId_whenCreating_thenShouldReject() {
        assertThatThrownBy(() -> new TransferResultEvent(
                        UUID.randomUUID(), TransferResultEvent.Outcome.SETTLED, UUID.randomUUID(), null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
