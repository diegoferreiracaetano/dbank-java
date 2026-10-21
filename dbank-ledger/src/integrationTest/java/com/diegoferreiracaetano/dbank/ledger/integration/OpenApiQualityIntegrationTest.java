package com.diegoferreiracaetano.dbank.ledger.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class OpenApiQualityIntegrationTest extends IntegrationTestBase {

    private final ObjectMapper json = new ObjectMapper();

    @Autowired
    MockMvc mockMvc;

    @Test
    void givenEveryOperation_whenReadingTheContract_thenShouldHaveSummaryAndDescribedResponses() throws Exception {
        var paths = contract().get("paths");

        paths.forEach(path -> path.forEach(operation -> {
            assertThat(operation.path("summary").asText()).isNotBlank();
            operation
                    .get("responses")
                    .forEach(response ->
                            assertThat(response.path("description").asText()).isNotBlank());
        }));
    }

    @Test
    void givenTransactionEndpoint_whenReadingTheContract_thenShouldDocumentEveryOutcome() throws Exception {
        var responses = contract().at("/paths/~1v1~1transactions/post/responses");

        for (var statusCode : List.of("204", "400", "404", "409", "422")) {
            assertThat(responses.has(statusCode)).as("response " + statusCode).isTrue();
        }
    }

    @Test
    void givenOpenAccount_whenReadingTheContract_thenShouldDocumentCreatedAndNotOk() throws Exception {
        var responses = contract().at("/paths/~1v1~1accounts/post/responses");

        assertThat(responses.has("201")).isTrue();
        assertThat(responses.has("200")).isFalse();
    }

    @Test
    void givenErrorResponses_whenReadingTheContract_thenShouldUseProblemJson() throws Exception {
        var notFound = contract().at("/paths/~1v1~1accounts~1{id}/get/responses/404/content");

        assertThat(notFound.has("application/problem+json")).isTrue();
    }

    @Test
    void givenStatementEndpoint_whenReadingTheContract_thenShouldDocumentThePagingLimits() throws Exception {
        var parameters = contract().at("/paths/~1v1~1accounts~1{id}~1entries/get/parameters");

        var size = findByName(parameters, "size");
        var page = findByName(parameters, "page");

        assertThat(size.at("/schema/minimum").asInt()).isEqualTo(1);
        assertThat(size.at("/schema/maximum").asInt()).isEqualTo(100);
        assertThat(page.at("/schema/minimum").asInt()).isZero();
    }

    @Test
    void givenGetAccount_whenReadingTheContract_thenShouldDeclareJsonResponse() throws Exception {
        var content = contract().at("/paths/~1v1~1accounts~1{id}/get/responses/200/content");

        assertThat(content.has("application/json")).isTrue();
    }

    @Test
    void givenDevProfile_whenOpeningSwaggerUi_thenShouldServeIt() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    private static JsonNode findByName(JsonNode parameters, String name) {
        for (var parameter : parameters) {
            if (name.equals(parameter.path("name").asText())) {
                return parameter;
            }
        }
        throw new AssertionError("Parameter not documented: " + name);
    }

    private JsonNode contract() throws Exception {
        var body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return json.readTree(body);
    }
}
