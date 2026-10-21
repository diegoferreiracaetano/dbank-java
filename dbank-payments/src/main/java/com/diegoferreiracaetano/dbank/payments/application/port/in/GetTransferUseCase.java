package com.diegoferreiracaetano.dbank.payments.application.port.in;

import com.diegoferreiracaetano.dbank.payments.domain.transfer.Transfer;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferId;

public interface GetTransferUseCase {

    Transfer get(TransferId id);
}
