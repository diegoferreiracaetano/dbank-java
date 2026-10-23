package com.diegoferreiracaetano.dbank.ledger.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.diegoferreiracaetano.dbank.ledger.application.port.out.AccountRepository;
import com.diegoferreiracaetano.dbank.ledger.domain.account.Account;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.shared.Money;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;

class TransferRequestedConsumerIntegrationTest extends IntegrationTestBase {

    private static final String TOPIC = "payments.transfer-requested.v1";

    @Autowired
    KafkaTemplate<String, String> kafka;

    @Autowired
    AccountRepository accounts;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void givenTransferRequestedEvent_whenConsumed_thenShouldMoveTheBalancesAndRecordSettled() throws Exception {
        var from = open(1_000);
        var to = open(0);
        var transferId = UUID.randomUUID();

        send(from, event(transferId, from, to, 300));

        awaitOutcome(transferId, "SETTLED");
        assertThat(balanceOf(from)).isEqualTo(700);
        assertThat(balanceOf(to)).isEqualTo(300);
    }

    @Test
    void givenSameEventDeliveredTwice_whenConsumed_thenShouldMoveTheMoneyOnce() throws Exception {
        var from = open(1_000);
        var to = open(0);
        var event = event(UUID.randomUUID(), from, to, 300);
        var marker = UUID.randomUUID();

        send(from, event);
        send(from, event);
        send(from, event(marker, from, to, 100));

        awaitOutcome(marker, "SETTLED");
        assertThat(balanceOf(from)).isEqualTo(600);
        assertThat(balanceOf(to)).isEqualTo(400);
    }

    @Test
    void givenInsufficientFunds_whenConsumed_thenShouldRecordRejectedAndKeepTheBalance() throws Exception {
        var from = open(100);
        var to = open(0);
        var transferId = UUID.randomUUID();

        send(from, event(transferId, from, to, 300));

        awaitOutcome(transferId, "REJECTED");
        assertThat(reasonOf(transferId)).isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(balanceOf(from)).isEqualTo(100);
    }

    @Test
    void givenUnknownAccount_whenConsumed_thenShouldRecordRejectedAsAccountNotFound() throws Exception {
        var from = open(1_000);
        var transferId = UUID.randomUUID();

        send(from, event(transferId, from, AccountId.generate(), 300));

        awaitOutcome(transferId, "REJECTED");
        assertThat(reasonOf(transferId)).isEqualTo("ACCOUNT_NOT_FOUND");
    }

    @Test
    void givenGarbageBeforeAValidEvent_whenConsumed_thenShouldStillProcessTheValidOne() throws Exception {
        var from = open(1_000);
        var to = open(0);
        var transferId = UUID.randomUUID();

        kafka.send(TOPIC, "garbage", "not json at all").get();
        kafka.send(TOPIC, "garbage", "{\"eventType\":\"Other\"}").get();
        send(from, event(transferId, from, to, 100));

        awaitOutcome(transferId, "SETTLED");
    }

    private AccountId open(long cents) {
        var id = AccountId.generate();
        accounts.save(new Account(id, Money.ofCents(cents)));
        return id;
    }

    private void send(AccountId key, String value) throws Exception {
        kafka.send(TOPIC, key.value().toString(), value).get();
    }

    private String event(UUID transferId, AccountId from, AccountId to, long cents) {
        return "{\"eventId\":\"" + UUID.randomUUID() + "\",\"eventType\":\"TransferRequested\","
                + "\"occurredAt\":\"2026-01-01T00:00:00Z\",\"payload\":{\"transferId\":\"" + transferId
                + "\",\"fromAccountId\":\"" + from.value() + "\",\"toAccountId\":\"" + to.value()
                + "\",\"amountCents\":" + cents + "}}";
    }

    private void awaitOutcome(UUID transferId, String status) {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(jdbc.queryForList(
                                "select status from transfer_outcome where transfer_id = ?", String.class, transferId))
                        .containsExactly(status));
    }

    private String reasonOf(UUID transferId) {
        return jdbc.queryForObject(
                "select reason from transfer_outcome where transfer_id = ?", String.class, transferId);
    }

    private long balanceOf(AccountId id) {
        return jdbc.queryForObject("select balance_cents from account where id = ?", Long.class, id.value());
    }
}
