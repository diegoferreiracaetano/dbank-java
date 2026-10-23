package com.diegoferreiracaetano.dbank.ledger.adapter.out.persistence.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
class OutboxEventEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String topic;

    @Column(name = "message_key", nullable = false, length = 64)
    private String messageKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    protected OutboxEventEntity() {}

    OutboxEventEntity(UUID id, String topic, String messageKey, String payload, Instant createdAt) {
        this.id = id;
        this.topic = topic;
        this.messageKey = messageKey;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    UUID getId() {
        return id;
    }

    String getTopic() {
        return topic;
    }

    String getMessageKey() {
        return messageKey;
    }

    String getPayload() {
        return payload;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
