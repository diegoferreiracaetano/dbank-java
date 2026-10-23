package com.diegoferreiracaetano.dbank.ledger.application.transfer;

public sealed interface TransferOutcome {

    record Settled() implements TransferOutcome {}

    record Rejected(RejectionReason reason) implements TransferOutcome {}
}
