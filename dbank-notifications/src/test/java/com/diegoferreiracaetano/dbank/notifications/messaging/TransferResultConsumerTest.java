package com.diegoferreiracaetano.dbank.notifications.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.diegoferreiracaetano.dbank.notifications.service.NotificationService;
import com.diegoferreiracaetano.dbank.notifications.service.TransferResultEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.converter.MessageConversionException;

class TransferResultConsumerTest {

    private final NotificationService service = mock(NotificationService.class);
    private final ObjectMapper mapper =
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final TransferResultConsumer consumer = new TransferResultConsumer(service, mapper);

    private final UUID eventId = UUID.randomUUID();
    private final UUID transferId = UUID.randomUUID();
    private final UUID account = UUID.randomUUID();

    @Test
    void givenSettledEvent_whenConsuming_thenShouldHandleASettledEvent() {
        consumer.onMessage(event("TransferSettled", eventId.toString(), transferId.toString(), null));

        var event = captured();
        assertThat(event.eventId()).isEqualTo(eventId);
        assertThat(event.outcome()).isEqualTo(TransferResultEvent.Outcome.SETTLED);
        assertThat(event.transferId()).isEqualTo(transferId);
        assertThat(event.accountId()).isEqualTo(account);
    }

    @Test
    void givenRejectedEvent_whenConsuming_thenShouldHandleARejectedEventWithTheReason() {
        consumer.onMessage(event("TransferRejected", eventId.toString(), transferId.toString(), "INSUFFICIENT_FUNDS"));

        var event = captured();
        assertThat(event.outcome()).isEqualTo(TransferResultEvent.Outcome.REJECTED);
        assertThat(event.reason()).isEqualTo("INSUFFICIENT_FUNDS");
    }

    @Test
    void givenNotJson_whenConsuming_thenShouldThrowMessageConversion() {
        assertThatThrownBy(() -> consumer.onMessage("not json at all")).isInstanceOf(MessageConversionException.class);

        verify(service, never()).handle(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void givenUnknownEventType_whenConsuming_thenShouldThrowMessageConversion() {
        assertThatThrownBy(() ->
                        consumer.onMessage(event("TransferReversed", eventId.toString(), transferId.toString(), null)))
                .isInstanceOf(MessageConversionException.class);
    }

    @Test
    void givenMissingEventId_whenConsuming_thenShouldThrowMessageConversion() {
        assertThatThrownBy(() -> consumer.onMessage("{\"eventType\":\"TransferSettled\",\"payload\":{\"transferId\":\""
                        + transferId + "\",\"fromAccountId\":\"" + account + "\"}}"))
                .isInstanceOf(MessageConversionException.class);
    }

    @Test
    void givenMissingPayload_whenConsuming_thenShouldThrowMessageConversion() {
        assertThatThrownBy(
                        () -> consumer.onMessage("{\"eventId\":\"" + eventId + "\",\"eventType\":\"TransferSettled\"}"))
                .isInstanceOf(MessageConversionException.class);
    }

    @Test
    void givenInvalidTransferId_whenConsuming_thenShouldThrowMessageConversion() {
        assertThatThrownBy(() -> consumer.onMessage(event("TransferSettled", eventId.toString(), "xyz", null)))
                .isInstanceOf(MessageConversionException.class);
    }

    @Test
    void givenNullValue_whenConsuming_thenShouldThrowMessageConversion() {
        assertThatThrownBy(() -> consumer.onMessage(null)).isInstanceOf(MessageConversionException.class);
    }

    private TransferResultEvent captured() {
        var captor = ArgumentCaptor.forClass(TransferResultEvent.class);
        verify(service).handle(captor.capture());
        return captor.getValue();
    }

    private String event(String type, String eventId, String transferId, String reason) {
        var reasonField = reason == null ? "" : ",\"reason\":\"" + reason + "\"";
        return "{\"eventId\":\"" + eventId + "\",\"eventType\":\"" + type + "\",\"payload\":{\"transferId\":\""
                + transferId + "\",\"fromAccountId\":\"" + account + "\"" + reasonField + "}}";
    }
}
