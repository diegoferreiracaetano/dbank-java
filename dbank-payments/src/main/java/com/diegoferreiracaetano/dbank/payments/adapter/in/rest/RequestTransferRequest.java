package com.diegoferreiracaetano.dbank.payments.adapter.in.rest;

import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyKey;
import com.diegoferreiracaetano.dbank.payments.application.port.in.RequestTransferCommand;
import com.diegoferreiracaetano.dbank.payments.domain.shared.Money;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.AccountId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

record RequestTransferRequest(
        @Schema(description = "Account to debit", example = "6f1c0a52-3d3e-4c1f-9d4b-2a7f6e0b9a11") @NotNull
        UUID fromAccountId,

        @Schema(description = "Account to credit", example = "0b8e5b7a-9c1d-4e0a-8f3a-5d2c7e1b4a90") @NotNull
        UUID toAccountId,

        @Schema(description = "Amount in cents; must be positive", example = "1000") @Positive
        long amountCents) {

    RequestTransferCommand toCommand(String idempotencyKey) {
        return new RequestTransferCommand(
                new IdempotencyKey(idempotencyKey),
                new AccountId(fromAccountId),
                new AccountId(toAccountId),
                Money.ofCents(amountCents));
    }
}
