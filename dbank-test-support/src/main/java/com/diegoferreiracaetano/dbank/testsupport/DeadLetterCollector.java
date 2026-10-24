package com.diegoferreiracaetano.dbank.testsupport;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.kafka.annotation.KafkaListener;

@TestConfiguration(proxyBeanMethods = false)
public class DeadLetterCollector {

    public static final String TOPIC_PROPERTY = "dbank.test.dead-letter-topic";

    private final List<ConsumerRecord<String, String>> records = new CopyOnWriteArrayList<>();

    @KafkaListener(topics = "${" + TOPIC_PROPERTY + "}", groupId = "dlt-#{T(java.util.UUID).randomUUID()}")
    void collect(ConsumerRecord<String, String> record) {
        records.add(record);
    }

    public List<String> valuesOf(String key) {
        return records.stream()
                .filter(record -> key.equals(record.key()))
                .map(ConsumerRecord::value)
                .toList();
    }

    public String headerOf(String key, String header) {
        return records.stream()
                .filter(record -> key.equals(record.key()))
                .findFirst()
                .map(record -> record.headers().lastHeader(header))
                .map(h -> new String(h.value(), StandardCharsets.UTF_8))
                .orElse(null);
    }
}
