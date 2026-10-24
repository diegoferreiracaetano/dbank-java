package com.diegoferreiracaetano.dbank.ledger.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.diegoferreiracaetano.dbank.testsupport.DeadLetterCollector;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;

@Import(DeadLetterCollector.class)
class DeadLetterIntegrationTest extends IntegrationTestBase {

    private static final String TOPIC = "payments.transfer-requested.v1";

    @Autowired
    KafkaTemplate<String, String> kafka;

    @Autowired
    DeadLetterCollector deadLetters;

    @Test
    void givenGarbage_whenConsuming_thenShouldGoStraightToTheDeadLetterTopicWithTheOriginalMessage() throws Exception {
        var key = UUID.randomUUID().toString();

        kafka.send(TOPIC, key, "not json at all").get();

        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(deadLetters.valuesOf(key)).containsExactly("not json at all"));
        assertThat(deadLetters.headerOf(key, KafkaHeaders.DLT_ORIGINAL_TOPIC)).isEqualTo(TOPIC);
    }

    @Test
    void givenEventOfTheWrongType_whenConsuming_thenShouldGoToTheDeadLetterTopic() throws Exception {
        var key = UUID.randomUUID().toString();
        var message = "{\"eventType\":\"Other\",\"payload\":{}}";

        kafka.send(TOPIC, key, message).get();

        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(deadLetters.valuesOf(key)).containsExactly(message));
    }
}
