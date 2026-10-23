package com.diegoferreiracaetano.dbank.ledger.integration;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.kafka.annotation.KafkaListener;

@TestConfiguration(proxyBeanMethods = false)
class DeadLetterCollector {

    private final List<ConsumerRecord<String, String>> records = new CopyOnWriteArrayList<>();

    @KafkaListener(
            topics = "payments.transfer-requested.v1.ledger.dlt",
            groupId = "dlt-#{T(java.util.UUID).randomUUID()}")
    void collect(ConsumerRecord<String, String> record) {
        records.add(record);
    }

    List<String> valuesOf(String key) {
        return records.stream()
                .filter(record -> key.equals(record.key()))
                .map(ConsumerRecord::value)
                .toList();
    }

    String headerOf(String key, String header) {
        return records.stream()
                .filter(record -> key.equals(record.key()))
                .findFirst()
                .map(record -> record.headers().lastHeader(header))
                .map(h -> new String(h.value(), StandardCharsets.UTF_8))
                .orElse(null);
    }
}
