package com.diegoferreiracaetano.dbank.payments.integration;

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
    void givenRequestTransfer_whenReadingTheContract_thenShouldDocumentAcceptedAndNotOk() throws Exception {
        var responses = contract().at("/paths/~1v1~1transfers/post/responses");

        for (var statusCode : List.of("202", "400", "422")) {
            assertThat(responses.has(statusCode)).as("response " + statusCode).isTrue();
        }
        assertThat(responses.has("200")).isFalse();
    }

    @Test
    void givenRequestTransfer_whenReadingTheContract_thenShouldDocumentTheReplayHeader() throws Exception {
        var headers = contract().at("/paths/~1v1~1transfers/post/responses/202/headers");

        assertThat(headers.has("Idempotency-Replayed")).isTrue();
        assertThat(headers.has("Location")).isTrue();
    }

    @Test
    void givenRequestTransfer_whenReadingTheContract_thenShouldRequireTheIdempotencyKeyOnce() throws Exception {
        var parameters = contract().at("/paths/~1v1~1transfers/post/parameters");

        var matches = 0;
        for (var parameter : parameters) {
            if ("Idempotency-Key".equals(parameter.path("name").asText())) {
                matches++;
                assertThat(parameter.path("in").asText()).isEqualTo("header");
                assertThat(parameter.path("required").asBoolean()).isTrue();
            }
        }
        assertThat(matches).isEqualTo(1);
    }

    @Test
    void givenGetTransfer_whenReadingTheContract_thenShouldDocumentNotFoundAsProblemJson() throws Exception {
        var content = contract().at("/paths/~1v1~1transfers~1{id}/get/responses/404/content");

        assertThat(content.has("application/problem+json")).isTrue();
    }

    @Test
    void givenDevProfile_whenOpeningSwaggerUi_thenShouldServeIt() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
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
