package com.diegoferreiracaetano.dbank.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

final class DbankApi {

    record TransferView(UUID id, String status, String failureReason) {}

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String ledger;
    private final String payments;
    private final String treasury;

    DbankApi(String ledger, String payments, String treasury) {
        this.ledger = ledger;
        this.payments = payments;
        this.treasury = treasury;
    }

    UUID openAccount() throws Exception {
        var response = send(post(ledger + "/v1/accounts", "").build());
        return UUID.fromString(response.get("id").asText());
    }

    void fund(UUID account, long cents) throws Exception {
        var body = """
                {"transactionId":"%s","fromAccountId":"%s","toAccountId":"%s","amountCents":%d}""".formatted(UUID.randomUUID(), treasury, account, cents);
        send(post(ledger + "/v1/transactions", body).build());
    }

    long balanceOf(UUID account) throws Exception {
        return send(get(ledger + "/v1/accounts/" + account)).get("balanceCents").asLong();
    }

    TransferView requestTransfer(String idempotencyKey, UUID from, UUID to, long cents) throws Exception {
        var body = """
                {"fromAccountId":"%s","toAccountId":"%s","amountCents":%d}""".formatted(from, to, cents);
        var request = post(payments + "/v1/transfers", body)
                .header("Idempotency-Key", idempotencyKey)
                .build();
        return view(send(request));
    }

    TransferView transfer(UUID id) throws Exception {
        return view(send(get(payments + "/v1/transfers/" + id)));
    }

    private HttpRequest get(String url) {
        return HttpRequest.newBuilder(URI.create(url)).GET().build();
    }

    private HttpRequest.Builder post(String url, String body) {
        return HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
    }

    private JsonNode send(HttpRequest request) throws Exception {
        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException(
                    request.method() + " " + request.uri() + " -> " + response.statusCode() + " " + response.body());
        }
        return response.body().isBlank() ? mapper.createObjectNode() : mapper.readTree(response.body());
    }

    private TransferView view(JsonNode json) {
        var reason = json.get("failureReason");
        return new TransferView(
                UUID.fromString(json.get("id").asText()),
                json.get("status").asText(),
                reason == null || reason.isNull() ? null : reason.asText());
    }
}
