package com.diegoferreiracaetano.dbank.payments.application.idempotency;

import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferId;

public record IdempotencyRecord(IdempotencyKey key, RequestFingerprint fingerprint, TransferId transferId) {}
