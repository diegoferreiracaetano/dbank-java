package com.diegoferreiracaetano.dbank.payments.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

abstract class TransferApiSupport extends IntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    private final UUID from = UUID.randomUUID();
    private final UUID to = UUID.randomUUID();

    protected ResultActions postTransfer(String key, long cents) throws Exception {
        return mockMvc.perform(post("/v1/transfers")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(cents)));
    }

    protected static String idOf(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    protected static String newKey() {
        return "key-" + UUID.randomUUID();
    }

    private String body(long cents) {
        return """
                {"fromAccountId":"%s","toAccountId":"%s","amountCents":%d}
                """.formatted(from, to, cents);
    }
}
