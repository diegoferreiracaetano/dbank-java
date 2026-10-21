package com.diegoferreiracaetano.dbank.payments.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.diegoferreiracaetano.dbank.payments.domain.shared.Money;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.AccountId;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.Transfer;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferId;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferNotFoundException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class GetTransferServiceTest {

    private final InMemoryTransferRepository transfers = new InMemoryTransferRepository();
    private final GetTransferService service = new GetTransferService(transfers);

    @Test
    void givenExistingTransfer_whenGetting_thenShouldReturnIt() {
        var transfer = Transfer.request(
                TransferId.generate(),
                AccountId.generate(),
                AccountId.generate(),
                Money.ofCents(100),
                Instant.parse("2026-01-01T00:00:00Z"));
        transfers.save(transfer);

        var result = service.get(transfer.id());

        assertThat(result).isEqualTo(transfer);
    }

    @Test
    void givenUnknownTransfer_whenGetting_thenShouldThrowTransferNotFound() {
        var id = TransferId.generate();

        assertThatThrownBy(() -> service.get(id)).isInstanceOf(TransferNotFoundException.class);
    }
}
