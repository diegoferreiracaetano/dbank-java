package com.diegoferreiracaetano.dbank.payments.adapter.in.messaging;

import com.diegoferreiracaetano.dbank.payments.application.port.in.ApplyTransferResultCommand;
import com.diegoferreiracaetano.dbank.payments.application.port.in.ApplyTransferResultUseCase;
import com.diegoferreiracaetano.dbank.payments.application.port.in.TransferResult;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.FailureReason;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.InvalidTransferTransitionException;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferId;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class TransferResultConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(TransferResultConsumer.class);

    private final ApplyTransferResultUseCase useCase;
    private final ObjectMapper mapper;

    TransferResultConsumer(ApplyTransferResultUseCase useCase, ObjectMapper mapper) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "ledger.transfer-result.v1")
    void onMessage(String value) {
        parse(value).ifPresent(this::apply);
    }

    private void apply(ApplyTransferResultCommand command) {
        try {
            useCase.apply(command);
        } catch (InvalidTransferTransitionException | TransferNotFoundException e) {
            LOG.error("Result conflicts with the transfer state; not retrying: {}", command, e);
        }
    }

    private Optional<ApplyTransferResultCommand> parse(String value) {
        try {
            var envelope = mapper.readValue(value, Envelope.class);
            var payload = envelope.payload();
            if (payload == null || envelope.eventType() == null) {
                LOG.warn("Discarding result with unexpected shape: {}", value);
                return Optional.empty();
            }
            var transferId = new TransferId(payload.transferId());
            var result = resultOf(envelope.eventType(), payload.reason());
            if (result.isEmpty()) {
                LOG.warn("Discarding result with unexpected shape: {}", value);
            }
            return result.map(r -> new ApplyTransferResultCommand(transferId, r));
        } catch (JsonProcessingException | IllegalArgumentException e) {
            LOG.warn("Discarding unreadable result: {}", value, e);
            return Optional.empty();
        }
    }

    private Optional<TransferResult> resultOf(String eventType, String reason) {
        if ("TransferSettled".equals(eventType)) {
            return Optional.of(new TransferResult.Settled());
        }
        if ("TransferRejected".equals(eventType) && reason != null) {
            return Optional.of(new TransferResult.Rejected(FailureReason.valueOf(reason)));
        }
        return Optional.empty();
    }

    private record Envelope(String eventType, Payload payload) {}

    private record Payload(UUID transferId, String reason) {}
}
