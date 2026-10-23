package com.diegoferreiracaetano.dbank.payments.adapter.in.messaging;

import com.diegoferreiracaetano.dbank.payments.application.port.in.ApplyTransferResultCommand;
import com.diegoferreiracaetano.dbank.payments.application.port.in.ApplyTransferResultUseCase;
import com.diegoferreiracaetano.dbank.payments.application.port.in.TransferResult;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.FailureReason;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferId;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.stereotype.Component;

@Component
class TransferResultConsumer {

    private final ApplyTransferResultUseCase useCase;
    private final ObjectMapper mapper;

    TransferResultConsumer(ApplyTransferResultUseCase useCase, ObjectMapper mapper) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "ledger.transfer-result.v1")
    void onMessage(String value) {
        useCase.apply(parse(value));
    }

    private ApplyTransferResultCommand parse(String value) {
        try {
            var envelope = mapper.readValue(value, Envelope.class);
            var payload = envelope.payload();
            if (payload == null || envelope.eventType() == null) {
                throw new MessageConversionException("Unexpected message shape");
            }
            return new ApplyTransferResultCommand(
                    new TransferId(payload.transferId()), resultOf(envelope.eventType(), payload.reason()));
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new MessageConversionException("Unreadable message", e);
        }
    }

    private TransferResult resultOf(String eventType, String reason) {
        if ("TransferSettled".equals(eventType)) {
            return new TransferResult.Settled();
        }
        if ("TransferRejected".equals(eventType) && reason != null) {
            return new TransferResult.Rejected(FailureReason.valueOf(reason));
        }
        throw new MessageConversionException("Unexpected event type " + eventType);
    }

    private record Envelope(String eventType, Payload payload) {}

    private record Payload(UUID transferId, String reason) {}
}
