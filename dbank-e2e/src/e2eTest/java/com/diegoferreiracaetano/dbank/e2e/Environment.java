package com.diegoferreiracaetano.dbank.e2e;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

final class Environment {

    private static final String TREASURY = "00000000-0000-0000-0000-000000000001";
    private static Environment instance;

    private final Infrastructure infrastructure = new Infrastructure();
    private ServiceProcess ledger;
    private ServiceProcess payments;
    private DbankApi api;

    static synchronized Environment get() throws Exception {
        if (instance == null) {
            var environment = new Environment();
            Runtime.getRuntime().addShutdownHook(new Thread(environment::stop));
            environment.start();
            instance = environment;
        }
        return instance;
    }

    private void start() throws Exception {
        infrastructure.start();
        ledger = ServiceProcess.start(
                "ledger",
                Path.of(System.getProperty("ledger.libs")),
                List.of(
                        "--spring.profiles.active=dev",
                        "--spring.datasource.url=" + infrastructure.jdbcUrl("ledger"),
                        "--spring.kafka.bootstrap-servers=" + infrastructure.kafkaBootstrap()));
        payments = ServiceProcess.start(
                "payments",
                Path.of(System.getProperty("payments.libs")),
                List.of(
                        "--spring.profiles.active=dev",
                        "--spring.datasource.url=" + infrastructure.jdbcUrl("payments"),
                        "--spring.data.redis.host=" + infrastructure.redisHost(),
                        "--spring.data.redis.port=" + infrastructure.redisPort(),
                        "--spring.kafka.bootstrap-servers=" + infrastructure.kafkaBootstrap()));
        ledger.awaitHealthy(Duration.ofMinutes(2));
        payments.awaitHealthy(Duration.ofMinutes(2));
        api = new DbankApi(ledger.baseUrl(), payments.baseUrl(), TREASURY);
    }

    private void stop() {
        if (payments != null) {
            payments.stop();
        }
        if (ledger != null) {
            ledger.stop();
        }
        infrastructure.stop();
    }

    DbankApi api() {
        return api;
    }

    Infrastructure infrastructure() {
        return infrastructure;
    }
}
