package com.diegoferreiracaetano.dbank.payments.adapter.out.persistence.transfer;

import com.diegoferreiracaetano.dbank.payments.application.port.out.TransferRepository;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.Transfer;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferId;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class TransferPersistenceAdapter implements TransferRepository {

    private final SpringDataTransferRepository repository;

    TransferPersistenceAdapter(SpringDataTransferRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(Transfer transfer) {
        repository.save(TransferMapper.toEntity(transfer));
    }

    @Override
    public Optional<Transfer> findById(TransferId id) {
        return repository.findById(id.value()).map(TransferMapper::toDomain);
    }
}
