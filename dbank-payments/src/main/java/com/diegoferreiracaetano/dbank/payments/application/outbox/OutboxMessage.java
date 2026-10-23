package com.diegoferreiracaetano.dbank.payments.application.outbox;

import java.time.Instant;
import java.util.UUID;

public record OutboxMessage(UUID id, String topic, String key, String payload, Instant createdAt) {}
