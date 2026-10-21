package com.diegoferreiracaetano.dbank.payments.application.port.in;

import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyKey;
import com.diegoferreiracaetano.dbank.payments.domain.shared.Money;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.AccountId;
import java.util.Objects;

public record RequestTransferCommand(IdempotencyKey idempotencyKey, AccountId from, AccountId to, Money amount) {

    public RequestTransferCommand {
        Objects.requireNonNull(idempotencyKey, "idempotencyKey is required");
        Objects.requireNonNull(from, "from is required");
        Objects.requireNonNull(to, "to is required");
        Objects.requireNonNull(amount, "amount is required");
    }
}
