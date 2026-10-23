package com.diegoferreiracaetano.dbank.payments.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegoferreiracaetano.dbank.payments.application.outbox.OutboxMessage;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PublishPendingEventsServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final InMemoryOutboxStore store = new InMemoryOutboxStore();
    private final RecordingEventPublisher publisher = new RecordingEventPublisher();
    private final PublishPendingEventsService service = new PublishPendingEventsService(
            store, publisher, new DirectTransactionRunner(), Clock.fixed(NOW, ZoneOffset.UTC), 2);

    @Test
    void givenPendingMessages_whenPublishing_thenShouldPublishInOrderAndMarkThemSent() {
        store.add(message("a"), message("b"));

        var sent = service.publishPending();

        assertThat(sent).isEqualTo(2);
        assertThat(publisher.published()).extracting(OutboxMessage::key).containsExactly("a", "b");
        assertThat(store.pending()).isEmpty();
        assertThat(store.sentAt()).isEqualTo(NOW);
    }

    @Test
    void givenBrokerFailingOnTheSecondMessage_whenPublishing_thenShouldKeepItPending() {
        store.add(message("a"), message("b"));
        publisher.failOn("b");

        var sent = service.publishPending();

        assertThat(sent).isEqualTo(1);
        assertThat(store.pending()).extracting(OutboxMessage::key).containsExactly("b");
    }

    @Test
    void givenBrokerFailingOnTheFirstMessage_whenPublishing_thenShouldMarkNothing() {
        store.add(message("a"), message("b"));
        publisher.failOn("a");

        var sent = service.publishPending();

        assertThat(sent).isZero();
        assertThat(store.sent()).isEmpty();
        assertThat(store.pending()).hasSize(2);
    }

    @Test
    void givenMoreMessagesThanTheBatchSize_whenPublishing_thenShouldPublishOnlyOneBatch() {
        store.add(message("a"), message("b"), message("c"));

        var sent = service.publishPending();

        assertThat(sent).isEqualTo(2);
        assertThat(store.pending()).extracting(OutboxMessage::key).containsExactly("c");
    }

    @Test
    void givenNothingPending_whenPublishing_thenShouldDoNothing() {
        assertThat(service.publishPending()).isZero();
        assertThat(store.sentAt()).isNull();
    }

    private OutboxMessage message(String key) {
        return new OutboxMessage(UUID.randomUUID(), "topic", key, "{}", NOW);
    }
}
