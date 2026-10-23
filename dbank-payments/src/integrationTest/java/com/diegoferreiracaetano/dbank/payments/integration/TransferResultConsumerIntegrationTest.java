package com.diegoferreiracaetano.dbank.payments.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyKey;
import com.diegoferreiracaetano.dbank.payments.application.port.in.RequestTransferCommand;
import com.diegoferreiracaetano.dbank.payments.application.port.in.RequestTransferUseCase;
import com.diegoferreiracaetano.dbank.payments.application.port.out.TransferRepository;
import com.diegoferreiracaetano.dbank.payments.domain.shared.Money;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.AccountId;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.FailureReason;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.Transfer;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferStatus;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

class TransferResultConsumerIntegrationTest extends IntegrationTestBase {

    private static final String TOPIC = "ledger.transfer-result.v1";

    @Autowired
    RequestTransferUseCase requestTransfer;

    @Autowired
    TransferRepository transfers;

    @Autowired
    KafkaTemplate<String, String> kafka;

    @Test
    void givenSettledEvent_whenConsumed_thenTheTransferShouldBecomeCompleted() throws Exception {
        var transfer = requested();

        send(transfer, settled(transfer));

        awaitStatus(transfer, TransferStatus.COMPLETED);
    }

    @Test
    void givenRejectedEvent_whenConsumed_thenTheTransferShouldFailWithTheReason() throws Exception {
        var transfer = requested();

        send(transfer, rejected(transfer, "INSUFFICIENT_FUNDS"));

        awaitStatus(transfer, TransferStatus.FAILED);
        assertThat(transfers.findById(transfer.id()).orElseThrow().failureReason())
                .isEqualTo(FailureReason.INSUFFICIENT_FUNDS);
    }

    @Test
    void givenSettledThenRejected_whenConsumed_thenTheTransferShouldStayCompleted() throws Exception {
        var transfer = requested();
        send(transfer, settled(transfer));
        awaitStatus(transfer, TransferStatus.COMPLETED);
        var marker = requestedFrom(transfer.from());

        send(transfer, rejected(transfer, "INSUFFICIENT_FUNDS"));
        send(transfer, settled(marker));

        awaitStatus(marker, TransferStatus.COMPLETED);
        assertThat(statusOf(transfer)).isEqualTo(TransferStatus.COMPLETED);
    }

    @Test
    void givenSameSettledEventTwice_whenConsumed_thenTheTransferShouldStayCompleted() throws Exception {
        var transfer = requested();
        var marker = requestedFrom(transfer.from());

        send(transfer, settled(transfer));
        send(transfer, settled(transfer));
        send(transfer, settled(marker));

        awaitStatus(marker, TransferStatus.COMPLETED);
        assertThat(statusOf(transfer)).isEqualTo(TransferStatus.COMPLETED);
    }

    @Test
    void givenGarbageBeforeAValidEvent_whenConsumed_thenShouldStillProcessTheValidOne() throws Exception {
        var transfer = requested();

        kafka.send(TOPIC, "garbage", "not json at all").get();
        kafka.send(TOPIC, "garbage", "{\"eventType\":\"TransferRejected\",\"payload\":{\"reason\":\"NOPE\"}}")
                .get();
        send(transfer, settled(transfer));

        awaitStatus(transfer, TransferStatus.COMPLETED);
    }

    private Transfer requested() {
        return requestedFrom(AccountId.generate());
    }

    private Transfer requestedFrom(AccountId from) {
        return requestTransfer
                .request(new RequestTransferCommand(
                        new IdempotencyKey(UUID.randomUUID().toString()),
                        from,
                        AccountId.generate(),
                        Money.ofCents(100)))
                .transfer();
    }

    private void send(Transfer transfer, String value) throws Exception {
        kafka.send(TOPIC, transfer.from().value().toString(), value).get();
    }

    private String settled(Transfer transfer) {
        return envelope("TransferSettled", "\"transferId\":\"" + transfer.id().value() + "\"");
    }

    private String rejected(Transfer transfer, String reason) {
        return envelope(
                "TransferRejected", "\"transferId\":\"" + transfer.id().value() + "\",\"reason\":\"" + reason + "\"");
    }

    private String envelope(String type, String payload) {
        return "{\"eventId\":\"" + UUID.randomUUID() + "\",\"eventType\":\"" + type
                + "\",\"occurredAt\":\"2026-01-01T00:00:00Z\",\"payload\":{" + payload + "}}";
    }

    private TransferStatus statusOf(Transfer transfer) {
        return transfers.findById(transfer.id()).orElseThrow().status();
    }

    private void awaitStatus(Transfer transfer, TransferStatus status) {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(statusOf(transfer)).isEqualTo(status));
    }
}
