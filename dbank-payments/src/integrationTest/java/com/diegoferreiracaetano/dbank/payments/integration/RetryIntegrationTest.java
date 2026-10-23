package com.diegoferreiracaetano.dbank.payments.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyKey;
import com.diegoferreiracaetano.dbank.payments.application.port.in.ApplyTransferResultUseCase;
import com.diegoferreiracaetano.dbank.payments.application.port.in.RequestTransferCommand;
import com.diegoferreiracaetano.dbank.payments.application.port.in.RequestTransferUseCase;
import com.diegoferreiracaetano.dbank.payments.application.port.out.TransferRepository;
import com.diegoferreiracaetano.dbank.payments.domain.shared.Money;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.AccountId;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.Transfer;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferStatus;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@Import(DeadLetterCollector.class)
@TestPropertySource(properties = {"dbank.kafka.retry.initial-interval=100ms", "dbank.kafka.retry.max-elapsed-time=2s"})
class RetryIntegrationTest extends IntegrationTestBase {

    private static final String TOPIC = "ledger.transfer-result.v1";

    @MockitoSpyBean
    ApplyTransferResultUseCase useCase;

    @Autowired
    RequestTransferUseCase requestTransfer;

    @Autowired
    TransferRepository transfers;

    @Autowired
    KafkaTemplate<String, String> kafka;

    @Autowired
    DeadLetterCollector deadLetters;

    @Test
    void givenTransientFailureTwice_whenConsuming_thenShouldRetryAndCompleteWithoutDeadLetter() throws Exception {
        var transfer = requested();
        var key = transfer.from().value().toString();
        var dbDown = new TransientDataAccessResourceException("db down");
        doThrow(dbDown).doThrow(dbDown).doCallRealMethod().when(useCase).apply(any());

        kafka.send(TOPIC, key, settled(transfer)).get();

        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(statusOf(transfer)).isEqualTo(TransferStatus.COMPLETED));
        verify(useCase, times(3)).apply(any());
        assertThat(deadLetters.valuesOf(key)).isEmpty();
    }

    @Test
    void givenFailureThatNeverEnds_whenConsuming_thenShouldEndInTheDeadLetterTopicWithTheOriginalMessage()
            throws Exception {
        var transfer = requested();
        var key = transfer.from().value().toString();
        var message = settled(transfer);
        doThrow(new TransientDataAccessResourceException("db down"))
                .when(useCase)
                .apply(any());

        kafka.send(TOPIC, key, message).get();

        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(deadLetters.valuesOf(key)).containsExactly(message));
        assertThat(deadLetters.headerOf(key, KafkaHeaders.DLT_ORIGINAL_TOPIC)).isEqualTo(TOPIC);
        assertThat(statusOf(transfer)).isEqualTo(TransferStatus.PENDING);
    }

    private Transfer requested() {
        return requestTransfer
                .request(new RequestTransferCommand(
                        new IdempotencyKey(UUID.randomUUID().toString()),
                        AccountId.generate(),
                        AccountId.generate(),
                        Money.ofCents(100)))
                .transfer();
    }

    private String envelope(String type, String payload) {
        return "{\"eventId\":\"" + UUID.randomUUID() + "\",\"eventType\":\"" + type
                + "\",\"occurredAt\":\"2026-01-01T00:00:00Z\",\"payload\":{" + payload + "}}";
    }

    private String settled(Transfer transfer) {
        return envelope("TransferSettled", "\"transferId\":\"" + transfer.id().value() + "\"");
    }

    private String rejected(Transfer transfer, String reason) {
        return envelope(
                "TransferRejected", "\"transferId\":\"" + transfer.id().value() + "\",\"reason\":\"" + reason + "\"");
    }

    private TransferStatus statusOf(Transfer transfer) {
        return transfers.findById(transfer.id()).orElseThrow().status();
    }
}
