package com.diegoferreiracaetano.dbank.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OutboxResilienceE2eTest {

    @Test
    void givenKafkaDownAtRequestTime_whenItComesBack_thenTheTransferShouldStillCompleteExactlyOnce() throws Exception {
        var environment = Environment.get();
        var api = environment.api();
        var from = api.openAccount();
        var to = api.openAccount();
        api.fund(from, 1_000);

        environment.infrastructure().pauseKafka();
        UUID transferId;
        try {
            var requested = api.requestTransfer(UUID.randomUUID().toString(), from, to, 300);
            transferId = requested.id();
            await().during(Duration.ofSeconds(5))
                    .atMost(Duration.ofSeconds(15))
                    .until(() -> api.transfer(requested.id()).status().equals("PENDING"));
        } finally {
            environment.infrastructure().resumeKafka();
        }

        await().atMost(Duration.ofSeconds(120))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(
                        () -> assertThat(api.transfer(transferId).status()).isEqualTo("COMPLETED"));
        assertThat(api.balanceOf(from)).isEqualTo(700);
        assertThat(api.balanceOf(to)).isEqualTo(300);
    }
}
