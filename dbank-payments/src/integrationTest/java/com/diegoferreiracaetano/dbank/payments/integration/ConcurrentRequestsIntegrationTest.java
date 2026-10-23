package com.diegoferreiracaetano.dbank.payments.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyKey;
import com.diegoferreiracaetano.dbank.payments.application.port.in.RequestTransferCommand;
import com.diegoferreiracaetano.dbank.payments.application.port.in.RequestTransferResult;
import com.diegoferreiracaetano.dbank.payments.application.port.out.IdempotencyStore;
import com.diegoferreiracaetano.dbank.payments.application.port.out.TransactionRunner;
import com.diegoferreiracaetano.dbank.payments.application.port.out.TransferEvents;
import com.diegoferreiracaetano.dbank.payments.application.port.out.TransferRepository;
import com.diegoferreiracaetano.dbank.payments.application.service.RequestTransferService;
import com.diegoferreiracaetano.dbank.payments.domain.shared.Money;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.AccountId;
import java.time.Clock;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ConcurrentRequestsIntegrationTest extends IntegrationTestBase {

    private static final int REQUESTS = 8;

    @Autowired
    TransferRepository transfers;

    @Autowired
    IdempotencyStore idempotency;

    @Autowired
    TransferEvents events;

    @Autowired
    TransactionRunner runner;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void givenEightConcurrentRequestsWithTheSameKey_whenRequesting_thenExactlyOneShouldCreateTheTransfer()
            throws Exception {
        var service = new RequestTransferService(transfers, idempotency, events, runner, Clock.systemUTC());
        var command = new RequestTransferCommand(
                new IdempotencyKey(UUID.randomUUID().toString()),
                AccountId.generate(),
                AccountId.generate(),
                Money.ofCents(100));
        var pool = Executors.newFixedThreadPool(REQUESTS);
        var start = new CountDownLatch(1);
        var futures = new ArrayList<Future<RequestTransferResult>>();

        for (int i = 0; i < REQUESTS; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                return service.request(command);
            }));
        }
        start.countDown();
        var results = new ArrayList<RequestTransferResult>();
        for (var future : futures) {
            results.add(future.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertThat(results).filteredOn(result -> !result.replayed()).hasSize(1);
        assertThat(results).filteredOn(RequestTransferResult::replayed).hasSize(REQUESTS - 1);
        assertThat(results)
                .extracting(result -> result.transfer().id())
                .containsOnly(results.get(0).transfer().id());
        assertThat(jdbc.queryForObject(
                        "select count(*) from idempotency_key where key = ?",
                        Integer.class,
                        command.idempotencyKey().value()))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "select count(*) from transfer where from_account_id = ?",
                        Integer.class,
                        command.from().value()))
                .isEqualTo(1);
    }
}
