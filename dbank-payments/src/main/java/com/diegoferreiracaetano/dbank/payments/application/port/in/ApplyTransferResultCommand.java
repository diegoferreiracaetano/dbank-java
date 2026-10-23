package com.diegoferreiracaetano.dbank.payments.application.port.in;

import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferId;
import java.util.Objects;

public record ApplyTransferResultCommand(TransferId transferId, TransferResult result) {

    public ApplyTransferResultCommand {
        Objects.requireNonNull(transferId, "transferId is required");
        Objects.requireNonNull(result, "result is required");
    }
}
