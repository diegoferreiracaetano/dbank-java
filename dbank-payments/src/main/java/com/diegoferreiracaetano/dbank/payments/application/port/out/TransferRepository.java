package com.diegoferreiracaetano.dbank.payments.application.port.out;

import com.diegoferreiracaetano.dbank.payments.domain.transfer.Transfer;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferId;
import java.util.Optional;

public interface TransferRepository {

    void save(Transfer transfer);

    Optional<Transfer> findById(TransferId id);
}
