package com.diegoferreiracaetano.dbank.ledger.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.diegoferreiracaetano.dbank.ledger.application.port.in.ProcessTransferRequestUseCase;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.AccountRepository;
import com.diegoferreiracaetano.dbank.ledger.domain.account.Account;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.shared.Money;
import com.diegoferreiracaetano.dbank.testsupport.DeadLetterCollector;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@Import(DeadLetterCollector.class)
@TestPropertySource(properties = {"dbank.kafka.retry.initial-interval=100ms", "dbank.kafka.retry.max-elapsed-time=2s"})
class RetryIntegrationTest extends IntegrationTestBase {

    private static final String TOPIC = "payments.transfer-requested.v1";

    @MockitoSpyBean
    ProcessTransferRequestUseCase useCase;

    @Autowired
    KafkaTemplate<String, String> kafka;

    @Autowired
    AccountRepository accounts;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    DeadLetterCollector deadLetters;

    @Test
    void givenTransientFailureTwice_whenConsuming_thenShouldRetryAndSettleWithoutDeadLetter() throws Exception {
        var from = open(1_000);
        var to = open(0);
        var transferId = UUID.randomUUID();
        var dbDown = new TransientDataAccessResourceException("db down");
        doThrow(dbDown).doThrow(dbDown).doCallRealMethod().when(useCase).process(any());

        kafka.send(TOPIC, from.value().toString(), event(transferId, from, to, 300))
                .get();

        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(outcomeOf(transferId)).isEqualTo("SETTLED"));
        verify(useCase, times(3)).process(any());
        assertThat(deadLetters.valuesOf(from.value().toString())).isEmpty();
    }

    @Test
    void givenFailureThatNeverEnds_whenConsuming_thenShouldEndInTheDeadLetterTopicWithTheOriginalMessage()
            throws Exception {
        var from = open(1_000);
        var to = open(0);
        var key = from.value().toString();
        var message = event(UUID.randomUUID(), from, to, 300);
        doThrow(new TransientDataAccessResourceException("db down"))
                .when(useCase)
                .process(any());

        kafka.send(TOPIC, key, message).get();

        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(deadLetters.valuesOf(key)).containsExactly(message));
        assertThat(deadLetters.headerOf(key, KafkaHeaders.DLT_ORIGINAL_TOPIC)).isEqualTo(TOPIC);
    }

    private AccountId open(long cents) {
        var id = AccountId.generate();
        accounts.save(new Account(id, Money.ofCents(cents)));
        return id;
    }

    private String outcomeOf(UUID transferId) {
        return jdbc
                .queryForList("select status from transfer_outcome where transfer_id = ?", String.class, transferId)
                .stream()
                .findFirst()
                .orElse(null);
    }

    private String event(UUID transferId, AccountId from, AccountId to, long cents) {
        return "{\"eventId\":\"" + UUID.randomUUID() + "\",\"eventType\":\"TransferRequested\","
                + "\"occurredAt\":\"2026-01-01T00:00:00Z\",\"payload\":{\"transferId\":\"" + transferId
                + "\",\"fromAccountId\":\"" + from.value() + "\",\"toAccountId\":\"" + to.value()
                + "\",\"amountCents\":" + cents + "}}";
    }
}
