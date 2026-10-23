package com.diegoferreiracaetano.dbank.ledger.application.outbox;

import java.time.Instant;
import java.util.UUID;

public record OutboxMessage(UUID id, String topic, String key, String payload, Instant createdAt) {}
