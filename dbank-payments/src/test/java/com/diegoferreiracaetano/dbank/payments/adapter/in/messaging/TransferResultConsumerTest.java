package com.diegoferreiracaetano.dbank.payments.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.diegoferreiracaetano.dbank.payments.application.port.in.ApplyTransferResultCommand;
import com.diegoferreiracaetano.dbank.payments.application.port.in.ApplyTransferResultUseCase;
import com.diegoferreiracaetano.dbank.payments.application.port.in.TransferResult;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.FailureReason;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.converter.MessageConversionException;

class TransferResultConsumerTest {

    private final List<ApplyTransferResultCommand> received = new ArrayList<>();
    private final ApplyTransferResultUseCase useCase = received::add;
    private final ObjectMapper mapper =
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final TransferResultConsumer consumer = new TransferResultConsumer(useCase, mapper);

    private final UUID transferId = UUID.randomUUID();

    @Test
    void givenSettledEvent_whenConsuming_thenShouldApplyASettledResult() {
        consumer.onMessage(event("TransferSettled", transferId.toString(), null));

        assertThat(received).hasSize(1);
        assertThat(received.get(0).transferId().value()).isEqualTo(transferId);
        assertThat(received.get(0).result()).isEqualTo(new TransferResult.Settled());
    }

    @Test
    void givenRejectedEvent_whenConsuming_thenShouldApplyARejectedResultWithTheReason() {
        consumer.onMessage(event("TransferRejected", transferId.toString(), "INSUFFICIENT_FUNDS"));

        assertThat(received.get(0).result()).isEqualTo(new TransferResult.Rejected(FailureReason.INSUFFICIENT_FUNDS));
    }

    @Test
    void givenNotJson_whenConsuming_thenShouldThrowMessageConversion() {
        assertThatThrownBy(() -> consumer.onMessage("not json at all")).isInstanceOf(MessageConversionException.class);

        assertThat(received).isEmpty();
    }

    @Test
    void givenUnknownEventType_whenConsuming_thenShouldThrowMessageConversion() {
        assertThatThrownBy(() -> consumer.onMessage(event("Other", transferId.toString(), null)))
                .isInstanceOf(MessageConversionException.class);
    }

    @Test
    void givenRejectedWithoutReason_whenConsuming_thenShouldThrowMessageConversion() {
        assertThatThrownBy(() -> consumer.onMessage(event("TransferRejected", transferId.toString(), null)))
                .isInstanceOf(MessageConversionException.class);
    }

    @Test
    void givenUnknownReason_whenConsuming_thenShouldThrowMessageConversion() {
        assertThatThrownBy(() -> consumer.onMessage(event("TransferRejected", transferId.toString(), "NOPE")))
                .isInstanceOf(MessageConversionException.class);
    }

    @Test
    void givenInvalidUuid_whenConsuming_thenShouldThrowMessageConversion() {
        assertThatThrownBy(() -> consumer.onMessage(event("TransferSettled", "xyz", null)))
                .isInstanceOf(MessageConversionException.class);
    }

    @Test
    void givenMissingPayload_whenConsuming_thenShouldThrowMessageConversion() {
        assertThatThrownBy(() -> consumer.onMessage("{\"eventType\":\"TransferSettled\"}"))
                .isInstanceOf(MessageConversionException.class);
    }

    private String event(String type, String id, String reason) {
        var reasonField = reason == null ? "" : ",\"reason\":\"" + reason + "\"";
        return "{\"eventType\":\"" + type + "\",\"payload\":{\"transferId\":\"" + id + "\"" + reasonField + "}}";
    }
}
