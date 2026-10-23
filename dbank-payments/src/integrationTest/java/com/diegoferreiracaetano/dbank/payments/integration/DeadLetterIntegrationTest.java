package com.diegoferreiracaetano.dbank.payments.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyKey;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;

@Import(DeadLetterCollector.class)
class DeadLetterIntegrationTest extends IntegrationTestBase {

    private static final String TOPIC = "ledger.transfer-result.v1";

    @Autowired
    RequestTransferUseCase requestTransfer;

    @Autowired
    TransferRepository transfers;

    @Autowired
    KafkaTemplate<String, String> kafka;

    @Autowired
    DeadLetterCollector deadLetters;

    @Test
    void givenGarbage_whenConsuming_thenShouldGoStraightToTheDeadLetterTopicWithTheOriginalMessage() throws Exception {
        var key = UUID.randomUUID().toString();

        kafka.send(TOPIC, key, "not json at all").get();

        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(deadLetters.valuesOf(key)).containsExactly("not json at all"));
        assertThat(deadLetters.headerOf(key, KafkaHeaders.DLT_ORIGINAL_TOPIC)).isEqualTo(TOPIC);
    }

    @Test
    void givenUnknownReason_whenConsuming_thenShouldGoToTheDeadLetterTopicAndLeaveTheTransferPending()
            throws Exception {
        var transfer = requested();
        var key = transfer.from().value().toString();
        var message = rejected(transfer, "NOPE");

        kafka.send(TOPIC, key, message).get();

        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(deadLetters.valuesOf(key)).containsExactly(message));
        assertThat(statusOf(transfer)).isEqualTo(TransferStatus.PENDING);
    }

    @Test
    void givenContradictoryResult_whenConsuming_thenShouldGoToTheDeadLetterTopicAndKeepTheState() throws Exception {
        var transfer = requested();
        var key = transfer.from().value().toString();
        kafka.send(TOPIC, key, settled(transfer)).get();
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(statusOf(transfer)).isEqualTo(TransferStatus.COMPLETED));
        var contradiction = rejected(transfer, "INSUFFICIENT_FUNDS");

        kafka.send(TOPIC, key, contradiction).get();

        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(deadLetters.valuesOf(key)).containsExactly(contradiction));
        assertThat(statusOf(transfer)).isEqualTo(TransferStatus.COMPLETED);
    }

    @Test
    void givenResultForUnknownTransfer_whenConsuming_thenShouldGoToTheDeadLetterTopic() throws Exception {
        var key = UUID.randomUUID().toString();
        var message = envelope("TransferSettled", "\"transferId\":\"" + UUID.randomUUID() + "\"");

        kafka.send(TOPIC, key, message).get();

        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(deadLetters.valuesOf(key)).containsExactly(message));
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
