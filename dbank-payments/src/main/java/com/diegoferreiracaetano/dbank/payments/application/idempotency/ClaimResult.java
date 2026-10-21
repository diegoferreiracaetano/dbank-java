package com.diegoferreiracaetano.dbank.payments.application.idempotency;

public sealed interface ClaimResult permits ClaimResult.Claimed, ClaimResult.Existing {

    record Claimed() implements ClaimResult {}

    record Existing(IdempotencyRecord record) implements ClaimResult {}
}
