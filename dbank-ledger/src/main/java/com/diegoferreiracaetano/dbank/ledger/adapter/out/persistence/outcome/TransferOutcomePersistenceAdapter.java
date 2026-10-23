package com.diegoferreiracaetano.dbank.ledger.adapter.out.persistence.outcome;

import com.diegoferreiracaetano.dbank.ledger.application.port.out.TransferOutcomes;
import com.diegoferreiracaetano.dbank.ledger.application.transfer.RejectionReason;
import com.diegoferreiracaetano.dbank.ledger.application.transfer.TransferOutcome;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransactionId;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class TransferOutcomePersistenceAdapter implements TransferOutcomes {

    private static final String SETTLED = "SETTLED";
    private static final String REJECTED = "REJECTED";

    private final SpringDataTransferOutcomeRepository repository;

    TransferOutcomePersistenceAdapter(SpringDataTransferOutcomeRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<TransferOutcome> find(LedgerTransactionId transferId) {
        return repository.findById(transferId.value()).map(this::toDomain);
    }

    @Override
    public void save(LedgerTransactionId transferId, TransferOutcome outcome, Instant processedAt) {
        var entity = switch (outcome) {
            case TransferOutcome.Settled() -> new TransferOutcomeEntity(transferId.value(), SETTLED, null, processedAt);
            case TransferOutcome.Rejected(var reason) ->
                new TransferOutcomeEntity(transferId.value(), REJECTED, reason.name(), processedAt);
        };
        repository.save(entity);
    }

    private TransferOutcome toDomain(TransferOutcomeEntity entity) {
        return SETTLED.equals(entity.getStatus())
                ? new TransferOutcome.Settled()
                : new TransferOutcome.Rejected(RejectionReason.valueOf(entity.getReason()));
    }
}
