package com.diegoferreiracaetano.dbank.payments.adapter.out.persistence.transfer;

import com.diegoferreiracaetano.dbank.payments.domain.shared.Money;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.AccountId;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.Transfer;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferId;

final class TransferMapper {

    private TransferMapper() {}

    static TransferEntity toEntity(Transfer transfer) {
        return new TransferEntity(
                transfer.id().value(),
                transfer.from().value(),
                transfer.to().value(),
                transfer.amount().cents(),
                transfer.status(),
                transfer.failureReason(),
                transfer.createdAt());
    }

    static Transfer toDomain(TransferEntity entity) {
        return new Transfer(
                new TransferId(entity.getId()),
                new AccountId(entity.getFromAccountId()),
                new AccountId(entity.getToAccountId()),
                Money.ofCents(entity.getAmountCents()),
                entity.getStatus(),
                entity.getFailureReason(),
                entity.getCreatedAt());
    }
}
