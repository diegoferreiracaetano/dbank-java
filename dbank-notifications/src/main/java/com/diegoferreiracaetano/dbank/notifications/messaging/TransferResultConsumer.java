package com.diegoferreiracaetano.dbank.notifications.messaging;

import com.diegoferreiracaetano.dbank.notifications.service.NotificationService;
import com.diegoferreiracaetano.dbank.notifications.service.TransferResultEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.stereotype.Component;

@Component
class TransferResultConsumer {

    private final NotificationService service;
    private final ObjectMapper mapper;

    TransferResultConsumer(NotificationService service, ObjectMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "ledger.transfer-result.v1")
    void onMessage(String value) {
        service.handle(parse(value));
    }

    private TransferResultEvent parse(String value) {
        try {
            var envelope = mapper.readValue(value, Envelope.class);
            var payload = envelope.payload();
            if (payload == null || envelope.eventType() == null) {
                throw new MessageConversionException("Unexpected message shape");
            }
            return new TransferResultEvent(
                    envelope.eventId(),
                    outcomeOf(envelope.eventType()),
                    payload.transferId(),
                    payload.fromAccountId(),
                    payload.reason());
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new MessageConversionException("Unreadable message", e);
        }
    }

    private TransferResultEvent.Outcome outcomeOf(String eventType) {
        return switch (eventType) {
            case "TransferSettled" -> TransferResultEvent.Outcome.SETTLED;
            case "TransferRejected" -> TransferResultEvent.Outcome.REJECTED;
            default -> throw new MessageConversionException("Unexpected event type " + eventType);
        };
    }

    private record Envelope(UUID eventId, String eventType, Payload payload) {}

    private record Payload(UUID transferId, UUID fromAccountId, String reason) {}
}
