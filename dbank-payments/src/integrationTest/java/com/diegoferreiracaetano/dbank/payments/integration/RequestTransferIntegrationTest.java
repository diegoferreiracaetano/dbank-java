package com.diegoferreiracaetano.dbank.payments.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyKey;
import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyKeyReuseException;
import com.diegoferreiracaetano.dbank.payments.application.port.in.RequestTransferCommand;
import com.diegoferreiracaetano.dbank.payments.application.port.out.IdempotencyStore;
import com.diegoferreiracaetano.dbank.payments.application.port.out.TransactionRunner;
import com.diegoferreiracaetano.dbank.payments.application.port.out.TransferRepository;
import com.diegoferreiracaetano.dbank.payments.application.service.RequestTransferService;
import com.diegoferreiracaetano.dbank.payments.domain.shared.Money;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.AccountId;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferStatus;
import java.time.Clock;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class RequestTransferIntegrationTest extends IntegrationTestBase {

    @Autowired
    TransferRepository transfers;

    @Autowired
    IdempotencyStore idempotency;

    @Autowired
    TransactionRunner runner;

    @Autowired
    JdbcTemplate jdbc;

    private final AccountId from = AccountId.generate();
    private final AccountId to = AccountId.generate();

    @Test
    void givenNewRequest_whenRequesting_thenShouldPersistAPendingTransfer() {
        var result = service().request(command(newKey(), 100));

        assertThat(result.replayed()).isFalse();
        assertThat(transfers.findById(result.transfer().id()).orElseThrow().status())
                .isEqualTo(TransferStatus.PENDING);
        assertThat(transferCount()).isEqualTo(1);
    }

    @Test
    void givenSameKeyAndSameRequest_whenRequestingAgain_thenShouldReplayTheOriginal() {
        var key = newKey();
        var first = service().request(command(key, 100));

        var second = service().request(command(key, 100));

        assertThat(second.replayed()).isTrue();
        assertThat(second.transfer().id()).isEqualTo(first.transfer().id());
        assertThat(transferCount()).isEqualTo(1);
    }

    @Test
    void givenSameKeyAndADifferentAmount_whenRequesting_thenShouldRejectAndKeepOneTransfer() {
        var key = newKey();
        service().request(command(key, 100));

        assertThatThrownBy(() -> service().request(command(key, 200))).isInstanceOf(IdempotencyKeyReuseException.class);

        assertThat(transferCount()).isEqualTo(1);
    }

    @Test
    void givenInvalidRequest_whenRequesting_thenShouldNotConsumeTheKey() {
        var key = newKey();
        var invalid = new RequestTransferCommand(key, from, from, Money.ofCents(100));

        assertThatThrownBy(() -> service().request(invalid)).isInstanceOf(IllegalArgumentException.class);

        assertThat(jdbc.queryForObject(
                        "select count(*) from idempotency_key where key = ?", Integer.class, key.value()))
                .isZero();
    }

    @Test
    void givenTransferCompletedMeanwhile_whenReplaying_thenShouldReturnTheCurrentState() {
        var key = newKey();
        var first = service().request(command(key, 100));
        transfers.save(first.transfer().complete());

        var replay = service().request(command(key, 100));

        assertThat(replay.replayed()).isTrue();
        assertThat(replay.transfer().status()).isEqualTo(TransferStatus.COMPLETED);
    }

    private RequestTransferService service() {
        return new RequestTransferService(transfers, idempotency, runner, Clock.systemUTC());
    }

    private RequestTransferCommand command(IdempotencyKey key, long cents) {
        return new RequestTransferCommand(key, from, to, Money.ofCents(cents));
    }

    private IdempotencyKey newKey() {
        return new IdempotencyKey(UUID.randomUUID().toString());
    }

    private Integer transferCount() {
        return jdbc.queryForObject(
                "select count(*) from transfer where from_account_id = ?", Integer.class, from.value());
    }
}
