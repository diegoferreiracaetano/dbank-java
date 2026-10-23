package com.diegoferreiracaetano.dbank.e2e;

import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

final class ServiceProcess {

    private final String name;
    private final Process process;
    private final int port;

    private ServiceProcess(String name, Process process, int port) {
        this.name = name;
        this.process = process;
        this.port = port;
    }

    static ServiceProcess start(String name, Path libsDir, List<String> arguments) throws IOException {
        var port = freePort();
        var java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        var command = new ArrayList<>(List.of(java, "-jar", bootJar(libsDir).toString(), "--server.port=" + port));
        command.addAll(arguments);
        var log = Path.of("build", "e2e", name + ".log");
        Files.createDirectories(log.getParent());
        var process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(log.toFile())
                .start();
        return new ServiceProcess(name, process, port);
    }

    String baseUrl() {
        return "http://localhost:" + port;
    }

    void awaitHealthy(Duration timeout) {
        var http = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder(URI.create(baseUrl() + "/actuator/health"))
                .build();
        await().atMost(timeout).pollInterval(Duration.ofSeconds(1)).until(() -> {
            if (!process.isAlive()) {
                throw new IllegalStateException(name + " exited early; see build/e2e/" + name + ".log");
            }
            try {
                return http.send(request, HttpResponse.BodyHandlers.discarding())
                                .statusCode()
                        == 200;
            } catch (IOException e) {
                return false;
            }
        });
    }

    void stop() {
        process.destroy();
    }

    private static Path bootJar(Path libsDir) throws IOException {
        try (var files = Files.list(libsDir)) {
            return files.filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .filter(path -> !path.getFileName().toString().endsWith("-plain.jar"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No boot jar in " + libsDir));
        }
    }

    private static int freePort() throws IOException {
        try (var socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
