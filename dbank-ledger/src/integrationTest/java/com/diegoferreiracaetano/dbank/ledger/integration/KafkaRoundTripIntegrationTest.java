package com.diegoferreiracaetano.dbank.ledger.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;

@Import(KafkaRoundTripIntegrationTest.Collector.class)
class KafkaRoundTripIntegrationTest extends IntegrationTestBase {

    private static final String TOPIC = "ledger.transfer-result.v1";

    @Autowired
    private KafkaTemplate<String, String> kafka;

    @Autowired
    private KafkaAdmin admin;

    @Autowired
    private Collector collector;

    @Test
    void givenTheServiceTopic_whenDescribing_thenShouldHaveThreePartitions() throws Exception {
        try (var client = AdminClient.create(admin.getConfigurationProperties())) {
            var description =
                    client.describeTopics(List.of(TOPIC)).allTopicNames().get(15, TimeUnit.SECONDS);

            assertThat(description.get(TOPIC).partitions()).hasSize(3);
        }
    }

    @Test
    void givenAMessage_whenProducing_thenAListenerShouldReceiveItWithTheSameKeyAndValue() throws Exception {
        var key = UUID.randomUUID().toString();

        kafka.send(TOPIC, key, "{\"hello\":\"kafka\"}").get(15, TimeUnit.SECONDS);

        var received = collector.records.poll(30, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.key()).isEqualTo(key);
        assertThat(received.value()).isEqualTo("{\"hello\":\"kafka\"}");
    }

    @Test
    void givenTwoMessagesWithTheSameKey_whenProducing_thenShouldLandOnTheSamePartition() throws Exception {
        var key = UUID.randomUUID().toString();

        var first = kafka.send(TOPIC, key, "1").get(15, TimeUnit.SECONDS);
        var second = kafka.send(TOPIC, key, "2").get(15, TimeUnit.SECONDS);

        assertThat(second.getRecordMetadata().partition())
                .isEqualTo(first.getRecordMetadata().partition());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Collector {

        private final BlockingQueue<ConsumerRecord<String, String>> records = new LinkedBlockingQueue<>();

        @KafkaListener(topics = TOPIC, groupId = "round-trip-#{T(java.util.UUID).randomUUID()}")
        void collect(ConsumerRecord<String, String> record) {
            records.add(record);
        }
    }
}
