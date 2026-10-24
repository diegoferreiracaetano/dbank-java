package com.diegoferreiracaetano.dbank.payments.integration;

import com.diegoferreiracaetano.dbank.testsupport.Containers;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;

@TestConfiguration(proxyBeanMethods = false)
class ContainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return Containers.postgres();
    }

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redis() {
        return Containers.redis();
    }

    @Bean
    @ServiceConnection
    KafkaContainer kafka() {
        return Containers.kafka();
    }
}
