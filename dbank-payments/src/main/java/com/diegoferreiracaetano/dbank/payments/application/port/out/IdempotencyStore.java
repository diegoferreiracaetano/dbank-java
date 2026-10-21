package com.diegoferreiracaetano.dbank.payments.application.port.out;

import com.diegoferreiracaetano.dbank.payments.application.idempotency.ClaimResult;
import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyRecord;

public interface IdempotencyStore {

    ClaimResult claim(IdempotencyRecord candidate);
}
