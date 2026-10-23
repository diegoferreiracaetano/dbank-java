package com.diegoferreiracaetano.dbank.ledger.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.diegoferreiracaetano.dbank.ledger.application.port.in.PostTransferCommand;
import com.diegoferreiracaetano.dbank.ledger.application.port.in.ProcessTransferRequestUseCase;
import com.diegoferreiracaetano.dbank.ledger.application.transfer.TransferOutcome;
import com.diegoferreiracaetano.dbank.ledger.domain.shared.Money;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.converter.MessageConversionException;

class TransferRequestedConsumerTest {

    private final List<PostTransferCommand> received = new ArrayList<>();
    private final ProcessTransferRequestUseCase useCase = command -> {
        received.add(command);
        return new TransferOutcome.Settled();
    };
    private final ObjectMapper mapper =
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final TransferRequestedConsumer consumer = new TransferRequestedConsumer(useCase, mapper);

    private final UUID transferId = UUID.randomUUID();
    private final UUID from = UUID.randomUUID();
    private final UUID to = UUID.randomUUID();

    @Test
    void givenValidEvent_whenConsuming_thenShouldCallTheUseCaseWithTheMappedCommand() {
        consumer.onMessage(event("TransferRequested", transferId.toString(), 300));

        assertThat(received).hasSize(1);
        var command = received.get(0);
        assertThat(command.transactionId().value()).isEqualTo(transferId);
        assertThat(command.from().value()).isEqualTo(from);
        assertThat(command.to().value()).isEqualTo(to);
        assertThat(command.amount()).isEqualTo(Money.ofCents(300));
    }

    @Test
    void givenNotJson_whenConsuming_thenShouldThrowMessageConversion() {
        assertThatThrownBy(() -> consumer.onMessage("not json at all")).isInstanceOf(MessageConversionException.class);

        assertThat(received).isEmpty();
    }

    @Test
    void givenWrongEventType_whenConsuming_thenShouldThrowMessageConversion() {
        assertThatThrownBy(() -> consumer.onMessage(event("Other", transferId.toString(), 300)))
                .isInstanceOf(MessageConversionException.class);

        assertThat(received).isEmpty();
    }

    @Test
    void givenMissingPayload_whenConsuming_thenShouldThrowMessageConversion() {
        assertThatThrownBy(() -> consumer.onMessage("{\"eventType\":\"TransferRequested\"}"))
                .isInstanceOf(MessageConversionException.class);
    }

    @Test
    void givenInvalidUuid_whenConsuming_thenShouldThrowMessageConversion() {
        assertThatThrownBy(() -> consumer.onMessage(event("TransferRequested", "xyz", 300)))
                .isInstanceOf(MessageConversionException.class);
    }

    @Test
    void givenNullValue_whenConsuming_thenShouldThrowMessageConversion() {
        assertThatThrownBy(() -> consumer.onMessage(null)).isInstanceOf(MessageConversionException.class);
    }

    private String event(String type, String id, long cents) {
        return "{\"eventType\":\"" + type + "\",\"payload\":{\"transferId\":\"" + id
                + "\",\"fromAccountId\":\"" + from + "\",\"toAccountId\":\"" + to
                + "\",\"amountCents\":" + cents + "}}";
    }
}
