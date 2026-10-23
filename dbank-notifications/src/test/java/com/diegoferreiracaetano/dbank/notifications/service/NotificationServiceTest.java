package com.diegoferreiracaetano.dbank.notifications.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.diegoferreiracaetano.dbank.notifications.persistence.NotificationRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationServiceTest {

    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final NotificationSender sender = mock(NotificationSender.class);
    private final NotificationService service = new NotificationService(repository, sender);

    private final UUID eventId = UUID.randomUUID();
    private final UUID transferId = UUID.randomUUID();
    private final UUID accountId = UUID.randomUUID();

    @Test
    void givenFirstDelivery_whenHandling_thenShouldStoreAndSendTheMessage() {
        var event = settled();
        var message = "Your transfer " + transferId + " was completed.";
        given(repository.insertIfAbsent(eventId, transferId, accountId, message))
                .willReturn(1);

        service.handle(event);

        verify(sender).send(accountId, message);
    }

    @Test
    void givenRepeatedDelivery_whenHandling_thenShouldNotSendAgain() {
        given(repository.insertIfAbsent(any(), any(), any(), anyString())).willReturn(0);

        service.handle(settled());

        verify(sender, never()).send(any(), anyString());
    }

    @Test
    void givenSenderFailure_whenHandling_thenShouldPropagateSoTheTransactionRollsBack() {
        given(repository.insertIfAbsent(any(), any(), any(), anyString())).willReturn(1);
        var failure = new IllegalStateException("smtp down");
        org.mockito.Mockito.doThrow(failure).when(sender).send(any(), anyString());

        assertThatThrownBy(() -> service.handle(settled())).isSameAs(failure);
    }

    @Test
    void givenRejectedEvent_whenHandling_thenShouldStoreTheDeclinedMessage() {
        var event = new TransferResultEvent(
                eventId, TransferResultEvent.Outcome.REJECTED, transferId, accountId, "INSUFFICIENT_FUNDS");
        var message = "Your transfer " + transferId + " was declined: insufficient funds.";
        given(repository.insertIfAbsent(eventId, transferId, accountId, message))
                .willReturn(1);

        service.handle(event);

        verify(sender).send(accountId, message);
        assertThat(message).contains("insufficient funds");
    }

    private TransferResultEvent settled() {
        return new TransferResultEvent(eventId, TransferResultEvent.Outcome.SETTLED, transferId, accountId, null);
    }
}
