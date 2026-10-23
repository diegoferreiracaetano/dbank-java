package com.diegoferreiracaetano.dbank.ledger.adapter.in.messaging;

import com.diegoferreiracaetano.dbank.ledger.application.port.in.PostTransferCommand;
import com.diegoferreiracaetano.dbank.ledger.application.port.in.ProcessTransferRequestUseCase;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransactionId;
import com.diegoferreiracaetano.dbank.ledger.domain.shared.Money;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.stereotype.Component;

@Component
class TransferRequestedConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(TransferRequestedConsumer.class);

    private final ProcessTransferRequestUseCase useCase;
    private final ObjectMapper mapper;

    TransferRequestedConsumer(ProcessTransferRequestUseCase useCase, ObjectMapper mapper) {
        this.useCase = useCase;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "payments.transfer-requested.v1")
    void onMessage(String value) {
        var command = parse(value);
        var outcome = useCase.process(command);
        LOG.info("Transfer {} processed: {}", command.transactionId().value(), outcome);
    }

    private PostTransferCommand parse(String value) {
        try {
            var envelope = mapper.readValue(value, Envelope.class);
            if (!"TransferRequested".equals(envelope.eventType()) || envelope.payload() == null) {
                throw new MessageConversionException("Unexpected message shape");
            }
            var payload = envelope.payload();
            return new PostTransferCommand(
                    new LedgerTransactionId(payload.transferId()),
                    new AccountId(payload.fromAccountId()),
                    new AccountId(payload.toAccountId()),
                    Money.ofCents(payload.amountCents()));
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new MessageConversionException("Unreadable message", e);
        }
    }

    private record Envelope(String eventType, Payload payload) {}

    private record Payload(UUID transferId, UUID fromAccountId, UUID toAccountId, long amountCents) {}
}
