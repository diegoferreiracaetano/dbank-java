package com.diegoferreiracaetano.dbank.ledger.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegoferreiracaetano.dbank.ledger.application.port.in.PostTransferCommand;
import com.diegoferreiracaetano.dbank.ledger.application.port.in.PostTransferUseCase;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.AccountRepository;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.ConcurrentUpdateException;
import com.diegoferreiracaetano.dbank.ledger.domain.account.Account;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransactionId;
import com.diegoferreiracaetano.dbank.ledger.domain.shared.Money;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ConcurrentTransfersIntegrationTest extends IntegrationTestBase {

    private static final int TRANSFERS = 8;
    private static final int MAX_ATTEMPTS = 50;

    @Autowired
    PostTransferUseCase postTransfer;

    @Autowired
    AccountRepository accounts;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void givenEightConcurrentTransfers_whenEachRetriesOnConflict_thenShouldApplyAllWithoutLosingUpdates()
            throws Exception {
        var from = AccountId.generate();
        var to = AccountId.generate();
        accounts.save(new Account(from, Money.ofCents(1000)));
        accounts.save(new Account(to, Money.ZERO));
        var pool = Executors.newFixedThreadPool(TRANSFERS);
        var start = new CountDownLatch(1);
        var futures = new ArrayList<Future<?>>();

        for (int i = 0; i < TRANSFERS; i++) {
            var command = new PostTransferCommand(LedgerTransactionId.generate(), from, to, Money.ofCents(100));
            futures.add(pool.submit(() -> {
                start.await();
                postWithRetry(command);
                return null;
            }));
        }
        start.countDown();
        for (var future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(accounts.findById(from).orElseThrow().balance()).isEqualTo(Money.ofCents(200));
        assertThat(accounts.findById(to).orElseThrow().balance()).isEqualTo(Money.ofCents(800));
        assertThat(jdbc.queryForObject(
                        "select count(*) from ledger_entry where account_id = ?", Integer.class, from.value()))
                .isEqualTo(TRANSFERS);
    }

    private void postWithRetry(PostTransferCommand command) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                postTransfer.post(command);
                return;
            } catch (ConcurrentUpdateException e) {
                continue;
            }
        }
        throw new IllegalStateException("Transfer did not succeed after " + MAX_ATTEMPTS + " attempts");
    }
}
