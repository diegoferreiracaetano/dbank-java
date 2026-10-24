package com.diegoferreiracaetano.dbank.notifications.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@Import(DeadLetterCollector.class)
@TestPropertySource(properties = {"dbank.kafka.retry.initial-interval=100ms", "dbank.kafka.retry.max-elapsed-time=2s"})
class RetryIntegrationTest extends IntegrationTestBase {

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
    void givenSenderFailingTwice_whenConsuming_thenShouldRetryAndStoreOneNotification() throws Exception {
        var eventId = UUID.randomUUID();
        var smtpDown = new IllegalStateException("smtp down");
        doThrow(smtpDown).doThrow(smtpDown).doCallRealMethod().when(sender).send(any(), any());

        send(account, event(eventId, "TransferSettled", UUID.randomUUID(), account, null));

        awaitNotification(eventId);
        verify(sender, times(3)).send(any(), any());
        assertThat(countFor(eventId)).isEqualTo(1);
    }

    @Test
    void givenSenderThatNeverRecovers_whenConsuming_thenShouldEndInTheDeadLetterTopicWithNoRowStored()
            throws Exception {
        var eventId = UUID.randomUUID();
        var message = event(eventId, "TransferSettled", UUID.randomUUID(), account, null);
        doThrow(new IllegalStateException("smtp down")).when(sender).send(any(), any());

        send(account, message);

        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() ->
                        assertThat(deadLetters.valuesOf(account.toString())).containsExactly(message));
        assertThat(countFor(eventId)).isZero();
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
