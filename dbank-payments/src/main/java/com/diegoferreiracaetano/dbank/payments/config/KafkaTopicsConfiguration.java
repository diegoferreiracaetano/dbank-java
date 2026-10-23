package com.diegoferreiracaetano.dbank.payments.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration(proxyBeanMethods = false)
class KafkaTopicsConfiguration {

    static final String TRANSFER_RESULT = "ledger.transfer-result.v1";
    static final String TRANSFER_REQUESTED = "payments.transfer-requested.v1";

    @Bean
    NewTopic transferRequestedTopic() {
        return TopicBuilder.name(TRANSFER_REQUESTED).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic transferResultTopic() {
        return TopicBuilder.name(TRANSFER_RESULT).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic transferResultDeadLetterTopic() {
        return TopicBuilder.name(TRANSFER_RESULT + ".dlt")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
