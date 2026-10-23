package com.diegoferreiracaetano.dbank.ledger.application.port.in;

import com.diegoferreiracaetano.dbank.ledger.application.transfer.TransferOutcome;

public interface ProcessTransferRequestUseCase {

    TransferOutcome process(PostTransferCommand command);
}
