package com.diegoferreiracaetano.dbank.payments.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

class TransferApiIntegrationTest extends IntegrationTestBase {

    private final UUID from = UUID.randomUUID();
    private final UUID to = UUID.randomUUID();

    @Autowired
    MockMvc mockMvc;

    @Test
    void givenNewRequest_whenPosting_thenShouldReturnAcceptedAndBeReadableByGet() throws Exception {
        var result = postTransfer("key-" + UUID.randomUUID(), 100)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        mockMvc.perform(get(result.getResponse().getHeader("Location")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amountCents").value(100))
                .andExpect(jsonPath("$.id").value(idOf(result)));
    }

    @Test
    void givenSameKeyAndBody_whenPostingAgain_thenShouldReplayTheSameTransfer() throws Exception {
        var key = "key-" + UUID.randomUUID();
        var first = idOf(postTransfer(key, 100).andReturn());

        postTransfer(key, 100)
                .andExpect(status().isAccepted())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andExpect(jsonPath("$.id").value(first));
    }

    @Test
    void givenSameKeyAndADifferentAmount_whenPosting_thenShouldReturnUnprocessableEntity() throws Exception {
        var key = "key-" + UUID.randomUUID();
        postTransfer(key, 100).andExpect(status().isAccepted());

        postTransfer(key, 200)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("urn:dbank:problem:idempotency-key-reuse"));
    }

    @Test
    void givenMissingKey_whenPosting_thenShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(100)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenUnknownTransfer_whenGetting_thenShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/v1/transfers/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }

    private ResultActions postTransfer(String key, long cents) throws Exception {
        return mockMvc.perform(post("/v1/transfers")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(cents)));
    }

    private String body(long cents) {
        return """
                {"fromAccountId":"%s","toAccountId":"%s","amountCents":%d}
                """.formatted(from, to, cents);
    }

    private static String idOf(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }
}
