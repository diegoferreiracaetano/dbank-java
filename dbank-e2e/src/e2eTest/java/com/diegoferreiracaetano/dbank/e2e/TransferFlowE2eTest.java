package com.diegoferreiracaetano.dbank.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TransferFlowE2eTest {

    private static DbankApi api;

    @BeforeAll
    static void startEnvironment() throws Exception {
        api = Environment.get().api();
    }

    @Test
    void givenFundedAccount_whenTransferring_thenShouldCompleteAndMoveTheMoney() throws Exception {
        var from = api.openAccount();
        var to = api.openAccount();
        api.fund(from, 1_000);

        var requested = api.requestTransfer(UUID.randomUUID().toString(), from, to, 300);

        assertThat(requested.status()).isEqualTo("PENDING");
        awaitStatus(requested.id(), "COMPLETED");
        assertThat(api.balanceOf(from)).isEqualTo(700);
        assertThat(api.balanceOf(to)).isEqualTo(300);
    }

    @Test
    void givenInsufficientFunds_whenTransferring_thenShouldFailWithTheReasonAndKeepTheBalances() throws Exception {
        var from = api.openAccount();
        var to = api.openAccount();
        api.fund(from, 100);

        var requested = api.requestTransfer(UUID.randomUUID().toString(), from, to, 300);

        awaitStatus(requested.id(), "FAILED");
        assertThat(api.transfer(requested.id()).failureReason()).isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(api.balanceOf(from)).isEqualTo(100);
        assertThat(api.balanceOf(to)).isZero();
    }

    @Test
    void givenUnknownDestination_whenTransferring_thenShouldFailWithAccountNotFound() throws Exception {
        var from = api.openAccount();
        api.fund(from, 1_000);

        var requested = api.requestTransfer(UUID.randomUUID().toString(), from, UUID.randomUUID(), 300);

        awaitStatus(requested.id(), "FAILED");
        assertThat(api.transfer(requested.id()).failureReason()).isEqualTo("ACCOUNT_NOT_FOUND");
        assertThat(api.balanceOf(from)).isEqualTo(1_000);
    }

    @Test
    void givenSameKeyTwice_whenTransferring_thenShouldCreateOneTransferAndMoveTheMoneyOnce() throws Exception {
        var from = api.openAccount();
        var to = api.openAccount();
        api.fund(from, 1_000);
        var key = UUID.randomUUID().toString();

        var first = api.requestTransfer(key, from, to, 300);
        var second = api.requestTransfer(key, from, to, 300);

        assertThat(second.id()).isEqualTo(first.id());
        awaitStatus(first.id(), "COMPLETED");
        assertThat(api.balanceOf(from)).isEqualTo(700);
        assertThat(api.balanceOf(to)).isEqualTo(300);
    }

    private void awaitStatus(UUID id, String status) {
        await().atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> assertThat(api.transfer(id).status()).isEqualTo(status));
    }
}
