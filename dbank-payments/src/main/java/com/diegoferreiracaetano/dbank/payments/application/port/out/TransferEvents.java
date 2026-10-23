package com.diegoferreiracaetano.dbank.payments.application.port.out;

import com.diegoferreiracaetano.dbank.payments.domain.transfer.Transfer;

public interface TransferEvents {

    void transferRequested(Transfer transfer);
}
