package com.diegoferreiracaetano.dbank.testsupport;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

public final class Containers {

    public static final String POSTGRES_IMAGE = "postgres:16";
    public static final String REDIS_IMAGE = "redis:7";
    public static final String KAFKA_IMAGE = "apache/kafka:3.8.0";

    private Containers() {}

    public static PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>(POSTGRES_IMAGE);
    }

    public static GenericContainer<?> redis() {
        return new GenericContainer<>(DockerImageName.parse(REDIS_IMAGE)).withExposedPorts(6379);
    }

    public static KafkaContainer kafka() {
        return new KafkaContainer(DockerImageName.parse(KAFKA_IMAGE));
    }
}
