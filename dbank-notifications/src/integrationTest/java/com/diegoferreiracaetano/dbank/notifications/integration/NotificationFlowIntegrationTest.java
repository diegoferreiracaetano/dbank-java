package com.diegoferreiracaetano.dbank.notifications.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.diegoferreiracaetano.dbank.notifications.service.NotificationSender;
import com.diegoferreiracaetano.dbank.testsupport.DeadLetterCollector;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@Import(DeadLetterCollector.class)
class NotificationFlowIntegrationTest extends IntegrationTestBase {

    private static final String TOPIC = "ledger.transfer-result.v1";

    @MockitoSpyBean
    NotificationSender sender;

    @Autowired
    KafkaTemplate<String, String> kafka;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    DeadLetterCollector deadLetters;

    private final UUID account = UUID.randomUUID();

    @Test
    void givenSettledEvent_whenConsumed_thenShouldStoreAndSendTheNotification() throws Exception {
        var eventId = UUID.randomUUID();
        var transferId = UUID.randomUUID();

        send(account, event(eventId, "TransferSettled", transferId, account, null));

        awaitNotification(eventId);
        assertThat(messageOf(eventId)).isEqualTo("Your transfer " + transferId + " was completed.");
        verify(sender).send(account, messageOf(eventId));
    }

    @Test
    void givenRejectedEvent_whenConsumed_thenShouldExplainTheReason() throws Exception {
        var eventId = UUID.randomUUID();
        var transferId = UUID.randomUUID();

        send(account, event(eventId, "TransferRejected", transferId, account, "INSUFFICIENT_FUNDS"));

        awaitNotification(eventId);
        assertThat(messageOf(eventId)).isEqualTo("Your transfer " + transferId + " was declined: insufficient funds.");
    }

    @Test
    void givenSameEventTwice_whenConsumed_thenShouldNotifyOnlyOnce() throws Exception {
        var eventId = UUID.randomUUID();
        var transferId = UUID.randomUUID();
        var marker = UUID.randomUUID();
        var event = event(eventId, "TransferSettled", transferId, account, null);

        send(account, event);
        send(account, event);
        send(account, event(marker, "TransferSettled", UUID.randomUUID(), account, null));

        awaitNotification(marker);
        assertThat(countFor(eventId)).isEqualTo(1);
        verify(sender, times(1)).send(eq(account), contains(transferId.toString()));
    }

    @Test
    void givenGarbage_whenConsuming_thenShouldGoToTheNotificationsDeadLetterTopic() throws Exception {
        var key = UUID.randomUUID().toString();

        kafka.send(TOPIC, key, "not json at all").get();

        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(deadLetters.valuesOf(key)).containsExactly("not json at all"));
    }

    private String event(UUID eventId, String type, UUID transferId, UUID account, String reason) {
        var reasonField = reason == null ? "" : ",\"reason\":\"" + reason + "\"";
        return "{\"eventId\":\"" + eventId + "\",\"eventType\":\"" + type
                + "\",\"occurredAt\":\"2026-01-01T00:00:00Z\",\"payload\":{\"transferId\":\"" + transferId
                + "\",\"fromAccountId\":\"" + account + "\"" + reasonField + "}}";
    }

    private void send(UUID account, String value) throws Exception {
        kafka.send(TOPIC, account.toString(), value).get();
    }

    private void awaitNotification(UUID eventId) {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(countFor(eventId)).isEqualTo(1));
    }

    private int countFor(UUID eventId) {
        return jdbc.queryForObject("select count(*) from notification where event_id = ?", Integer.class, eventId);
    }

    private String messageOf(UUID eventId) {
        return jdbc.queryForObject("select message from notification where event_id = ?", String.class, eventId);
    }
}
