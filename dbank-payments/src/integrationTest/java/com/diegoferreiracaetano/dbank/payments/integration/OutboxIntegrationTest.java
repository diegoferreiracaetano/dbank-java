package com.diegoferreiracaetano.dbank.payments.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyKey;
import com.diegoferreiracaetano.dbank.payments.application.port.in.RequestTransferCommand;
import com.diegoferreiracaetano.dbank.payments.application.port.in.RequestTransferUseCase;
import com.diegoferreiracaetano.dbank.payments.application.port.out.TransactionRunner;
import com.diegoferreiracaetano.dbank.payments.application.port.out.TransferEvents;
import com.diegoferreiracaetano.dbank.payments.application.port.out.TransferRepository;
import com.diegoferreiracaetano.dbank.payments.domain.shared.Money;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.AccountId;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.Transfer;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferId;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;

@Import(OutboxIntegrationTest.Collector.class)
class OutboxIntegrationTest extends IntegrationTestBase {

    @Autowired
    RequestTransferUseCase requestTransfer;

    @Autowired
    TransferRepository transfers;

    @Autowired
    TransferEvents events;

    @Autowired
    TransactionRunner runner;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    Collector collector;

    @Test
    void givenNewTransfer_whenRequesting_thenShouldStoreTheEventWithTheTransfer() {
        var from = AccountId.generate();

        var result = requestTransfer.request(command(from));

        var payload = jdbc.queryForObject(
                "select payload from outbox_event where message_key = ?",
                String.class,
                from.value().toString());
        assertThat(payload).contains(result.transfer().id().value().toString()).contains("\"amountCents\":100");
    }

    @Test
    void givenSameKeyTwice_whenRequesting_thenShouldStoreOnlyOneEvent() {
        var from = AccountId.generate();
        var command = command(from);

        requestTransfer.request(command);
        requestTransfer.request(command);

        assertThat(eventCount(from)).isEqualTo(1);
    }

    @Test
    void givenFailureAfterRecordingTheEvent_whenRunningInTransaction_thenShouldLeaveNoEventBehind() {
        var from = AccountId.generate();
        var transfer =
                Transfer.request(TransferId.generate(), from, AccountId.generate(), Money.ofCents(100), Instant.now());

        assertThatThrownBy(() -> runner.run(() -> {
                    transfers.save(transfer);
                    events.transferRequested(transfer);
                    throw new IllegalStateException("boom");
                }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(eventCount(from)).isZero();
    }

    @Test
    void givenPendingEvent_whenTheRelayRuns_thenShouldPublishToKafkaAndMarkItSent() {
        var from = AccountId.generate();

        var result = requestTransfer.request(command(from));

        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(collector.valuesOf(from))
                        .singleElement()
                        .asString()
                        .contains(result.transfer().id().value().toString()));
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(jdbc.queryForObject(
                                "select count(*) from outbox_event where message_key = ? and sent_at is not null",
                                Integer.class,
                                from.value().toString()))
                        .isEqualTo(1));
    }

    private RequestTransferCommand command(AccountId from) {
        return new RequestTransferCommand(
                new IdempotencyKey(UUID.randomUUID().toString()), from, AccountId.generate(), Money.ofCents(100));
    }

    private Integer eventCount(AccountId from) {
        return jdbc.queryForObject(
                "select count(*) from outbox_event where message_key = ?",
                Integer.class,
                from.value().toString());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Collector {

        private final List<ConsumerRecord<String, String>> records = new CopyOnWriteArrayList<>();

        @KafkaListener(topics = "payments.transfer-requested.v1", groupId = "outbox-#{T(java.util.UUID).randomUUID()}")
        void collect(ConsumerRecord<String, String> record) {
            records.add(record);
        }

        List<String> valuesOf(AccountId from) {
            return records.stream()
                    .filter(record -> record.key().equals(from.value().toString()))
                    .map(ConsumerRecord::value)
                    .toList();
        }
    }
}
