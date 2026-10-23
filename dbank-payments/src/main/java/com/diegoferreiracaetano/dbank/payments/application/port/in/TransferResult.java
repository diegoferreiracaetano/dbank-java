package com.diegoferreiracaetano.dbank.payments.application.port.in;

import com.diegoferreiracaetano.dbank.payments.domain.transfer.FailureReason;

public sealed interface TransferResult {

    record Settled() implements TransferResult {}

    record Rejected(FailureReason reason) implements TransferResult {}
}
