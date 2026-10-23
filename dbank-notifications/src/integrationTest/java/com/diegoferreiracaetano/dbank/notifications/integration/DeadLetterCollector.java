package com.diegoferreiracaetano.dbank.notifications.integration;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.kafka.annotation.KafkaListener;

@TestConfiguration(proxyBeanMethods = false)
class DeadLetterCollector {

    private final List<ConsumerRecord<String, String>> records = new CopyOnWriteArrayList<>();

    @KafkaListener(
            topics = "ledger.transfer-result.v1.notifications.dlt",
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
}
