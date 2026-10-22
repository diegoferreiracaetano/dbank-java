package com.diegoferreiracaetano.dbank.payments.adapter.out.persistence.transfer;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegoferreiracaetano.dbank.payments.domain.shared.Money;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.AccountId;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.FailureReason;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.Transfer;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TransferMapperTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void givenPendingTransfer_whenMappingToEntityAndBack_thenShouldReturnEqualTransfer() {
        var transfer = pending();

        var result = TransferMapper.toDomain(TransferMapper.toEntity(transfer));

        assertThat(result).isEqualTo(transfer);
    }

    @Test
    void givenFailedTransfer_whenMappingToEntityAndBack_thenShouldKeepTheFailureReason() {
        var transfer = pending().fail(FailureReason.INSUFFICIENT_FUNDS);

        var result = TransferMapper.toDomain(TransferMapper.toEntity(transfer));

        assertThat(result.failureReason()).isEqualTo(FailureReason.INSUFFICIENT_FUNDS);
    }

    @Test
    void givenTransfer_whenMappingToEntity_thenShouldKeepIdAccountsAndAmount() {
        var transfer = pending();

        var entity = TransferMapper.toEntity(transfer);

        assertThat(entity.getId()).isEqualTo(transfer.id().value());
        assertThat(entity.getFromAccountId()).isEqualTo(transfer.from().value());
        assertThat(entity.getToAccountId()).isEqualTo(transfer.to().value());
        assertThat(entity.getAmountCents()).isEqualTo(500);
    }

    private Transfer pending() {
        return Transfer.request(
                TransferId.generate(), AccountId.generate(), AccountId.generate(), Money.ofCents(500), NOW);
    }
}
