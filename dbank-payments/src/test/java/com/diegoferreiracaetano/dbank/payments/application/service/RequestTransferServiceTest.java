package com.diegoferreiracaetano.dbank.payments.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyKey;
import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyKeyReuseException;
import com.diegoferreiracaetano.dbank.payments.application.port.in.RequestTransferCommand;
import com.diegoferreiracaetano.dbank.payments.domain.shared.Money;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.AccountId;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RequestTransferServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final InMemoryTransferRepository transfers = new InMemoryTransferRepository();
    private final InMemoryIdempotencyStore idempotency = new InMemoryIdempotencyStore();
    private final InMemoryTransferEvents events = new InMemoryTransferEvents();
    private final RequestTransferService service =
            new RequestTransferService(transfers, idempotency, events, new DirectTransactionRunner(), CLOCK);

    private final AccountId from = AccountId.generate();
    private final AccountId to = AccountId.generate();

    @Test
    void givenNewKey_whenRequesting_thenShouldCreateAPendingTransferAndStoreIt() {
        var result = service.request(command("key-1", 100));

        assertThat(result.replayed()).isFalse();
        assertThat(result.transfer().status()).isEqualTo(TransferStatus.PENDING);
        assertThat(result.transfer().createdAt()).isEqualTo(NOW);
        assertThat(transfers.all()).containsExactly(result.transfer());
        assertThat(idempotency.size()).isEqualTo(1);
    }

    @Test
    void givenSameKeyAndSameRequest_whenRequestingAgain_thenShouldReturnTheOriginalWithoutCreatingAnother() {
        var first = service.request(command("key-1", 100));

        var second = service.request(command("key-1", 100));

        assertThat(second.replayed()).isTrue();
        assertThat(second.transfer().id()).isEqualTo(first.transfer().id());
        assertThat(transfers.all()).hasSize(1);
    }

    @Test
    void givenSameKeyAndADifferentAmount_whenRequesting_thenShouldRejectAndCreateNothingNew() {
        service.request(command("key-1", 100));

        assertThatThrownBy(() -> service.request(command("key-1", 200)))
                .isInstanceOf(IdempotencyKeyReuseException.class);

        assertThat(transfers.all()).hasSize(1);
    }

    @Test
    void givenDifferentKeysAndIdenticalRequests_whenRequesting_thenShouldCreateTwoTransfers() {
        var first = service.request(command("key-1", 100));

        var second = service.request(command("key-2", 100));

        assertThat(second.replayed()).isFalse();
        assertThat(second.transfer().id()).isNotEqualTo(first.transfer().id());
        assertThat(transfers.all()).hasSize(2);
    }

    @Test
    void givenInvalidRequest_whenRequesting_thenShouldNotConsumeTheKey() {
        var invalid = new RequestTransferCommand(new IdempotencyKey("key-1"), from, from, Money.ofCents(100));

        assertThatThrownBy(() -> service.request(invalid)).isInstanceOf(IllegalArgumentException.class);

        assertThat(idempotency.size()).isZero();
        assertThat(service.request(command("key-1", 100)).replayed()).isFalse();
    }

    @Test
    void givenTransferCompletedMeanwhile_whenReplaying_thenShouldReturnTheCurrentState() {
        var first = service.request(command("key-1", 100));
        transfers.save(first.transfer().complete());

        var replay = service.request(command("key-1", 100));

        assertThat(replay.replayed()).isTrue();
        assertThat(replay.transfer().status()).isEqualTo(TransferStatus.COMPLETED);
    }

    @Test
    void givenNewKey_whenRequesting_thenShouldRecordOneRequestedEvent() {
        var result = service.request(command("key-1", 100));

        assertThat(events.requested()).containsExactly(result.transfer());
    }

    @Test
    void givenSameKeyAndSameRequest_whenRequestingAgain_thenShouldNotRecordAnotherEvent() {
        service.request(command("key-1", 100));

        service.request(command("key-1", 100));

        assertThat(events.requested()).hasSize(1);
    }

    private RequestTransferCommand command(String key, long cents) {
        return new RequestTransferCommand(new IdempotencyKey(key), from, to, Money.ofCents(cents));
    }
}
