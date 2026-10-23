package com.diegoferreiracaetano.dbank.ledger.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.diegoferreiracaetano.dbank.ledger.application.port.in.PostTransferCommand;
import com.diegoferreiracaetano.dbank.ledger.application.port.in.ProcessTransferRequestUseCase;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.AccountRepository;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.TransactionRunner;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.TransferResultEvents;
import com.diegoferreiracaetano.dbank.ledger.application.transfer.TransferOutcome;
import com.diegoferreiracaetano.dbank.ledger.domain.account.Account;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransactionId;
import com.diegoferreiracaetano.dbank.ledger.domain.shared.Money;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
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
import org.springframework.kafka.core.KafkaTemplate;

@Import(OutboxIntegrationTest.Collector.class)
class OutboxIntegrationTest extends IntegrationTestBase {

    @Autowired
    ProcessTransferRequestUseCase processTransfer;

    @Autowired
    TransferResultEvents events;

    @Autowired
    TransactionRunner runner;

    @Autowired
    AccountRepository accounts;

    @Autowired
    KafkaTemplate<String, String> kafka;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    Collector collector;

    @Test
    void givenSettledTransfer_whenProcessing_thenShouldStoreTheEventWithoutAReasonField() throws Exception {
        var from = open(1_000);
        var to = open(0);

        processTransfer.process(command(from, to, 300));

        var json = singleEventOf(from);
        assertThat(json.get("eventType").asText()).isEqualTo("TransferSettled");
        assertThat(json.get("payload").has("reason")).isFalse();
    }

    @Test
    void givenRejectedTransfer_whenProcessing_thenShouldStoreTheReasonInTheEvent() throws Exception {
        var from = open(100);
        var to = open(0);

        processTransfer.process(command(from, to, 300));

        var json = singleEventOf(from);
        assertThat(json.get("eventType").asText()).isEqualTo("TransferRejected");
        assertThat(json.get("payload").get("reason").asText()).isEqualTo("INSUFFICIENT_FUNDS");
    }

    @Test
    void givenSameTransferTwice_whenProcessing_thenShouldStoreOnlyOneEvent() {
        var from = open(1_000);
        var to = open(0);
        var command = command(from, to, 300);

        processTransfer.process(command);
        processTransfer.process(command);

        assertThat(eventCount(from)).isEqualTo(1);
    }

    @Test
    void givenFailureAfterRecordingTheEvent_whenRunningInTransaction_thenShouldLeaveNoEventBehind() {
        var from = AccountId.generate();

        assertThatThrownBy(() -> runner.call(() -> {
                    events.transferProcessed(LedgerTransactionId.generate(), from, new TransferOutcome.Settled());
                    throw new IllegalStateException("boom");
                }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(eventCount(from)).isZero();
    }

    @Test
    void givenTransferRequestedOnKafka_whenProcessed_thenShouldPublishTheResultAndMarkItSent() throws Exception {
        var from = open(1_000);
        var to = open(0);
        var transferId = UUID.randomUUID();

        kafka.send("payments.transfer-requested.v1", from.value().toString(), requested(transferId, from, to, 300))
                .get();

        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(collector.valuesOf(from))
                        .singleElement()
                        .asString()
                        .contains("TransferSettled")
                        .contains(transferId.toString()));
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(sentCount(from)).isEqualTo(1));
    }

    private AccountId open(long cents) {
        var id = AccountId.generate();
        accounts.save(new Account(id, Money.ofCents(cents)));
        return id;
    }

    private PostTransferCommand command(AccountId from, AccountId to, long cents) {
        return new PostTransferCommand(LedgerTransactionId.generate(), from, to, Money.ofCents(cents));
    }

    private String requested(UUID transferId, AccountId from, AccountId to, long cents) {
        return "{\"eventId\":\"" + UUID.randomUUID() + "\",\"eventType\":\"TransferRequested\","
                + "\"occurredAt\":\"2026-01-01T00:00:00Z\",\"payload\":{\"transferId\":\"" + transferId
                + "\",\"fromAccountId\":\"" + from.value() + "\",\"toAccountId\":\"" + to.value()
                + "\",\"amountCents\":" + cents + "}}";
    }

    private JsonNode singleEventOf(AccountId from) throws Exception {
        var payloads = jdbc.queryForList(
                "select payload from outbox_event where message_key = ?",
                String.class,
                from.value().toString());
        assertThat(payloads).hasSize(1);
        return mapper.readTree(payloads.get(0));
    }

    private Integer eventCount(AccountId from) {
        return jdbc.queryForObject(
                "select count(*) from outbox_event where message_key = ?",
                Integer.class,
                from.value().toString());
    }

    private Integer sentCount(AccountId from) {
        return jdbc.queryForObject(
                "select count(*) from outbox_event where message_key = ? and sent_at is not null",
                Integer.class,
                from.value().toString());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Collector {

        private final List<ConsumerRecord<String, String>> records = new CopyOnWriteArrayList<>();

        @KafkaListener(topics = "ledger.transfer-result.v1", groupId = "outbox-#{T(java.util.UUID).randomUUID()}")
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
