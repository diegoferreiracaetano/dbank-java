package com.diegoferreiracaetano.dbank.payments.application.port.in;

import com.diegoferreiracaetano.dbank.payments.domain.transfer.Transfer;

public record RequestTransferResult(Transfer transfer, boolean replayed) {}
