package com.diegoferreiracaetano.dbank.e2e;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

final class Infrastructure {

    private final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("ledger")
            .withUsername("dbank")
            .withPassword("dbank");
    private final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7")).withExposedPorts(6379);
    private final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.0"));

    void start() throws Exception {
        Startables.deepStart(postgres, redis, kafka).join();
        var created = postgres.execInContainer("psql", "-U", "dbank", "-d", "ledger", "-c", "CREATE DATABASE payments");
        if (created.getExitCode() != 0) {
            throw new IllegalStateException("Could not create the payments database: " + created.getStderr());
        }
    }

    void stop() {
        kafka.stop();
        redis.stop();
        postgres.stop();
    }

    String jdbcUrl(String database) {
        return "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/" + database;
    }

    String kafkaBootstrap() {
        return kafka.getBootstrapServers();
    }

    String redisHost() {
        return redis.getHost();
    }

    int redisPort() {
        return redis.getMappedPort(6379);
    }

    void pauseKafka() {
        kafka.getDockerClient().pauseContainerCmd(kafka.getContainerId()).exec();
    }

    void resumeKafka() {
        kafka.getDockerClient().unpauseContainerCmd(kafka.getContainerId()).exec();
    }
}
