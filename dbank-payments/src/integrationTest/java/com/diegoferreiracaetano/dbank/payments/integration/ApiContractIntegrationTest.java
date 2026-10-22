package com.diegoferreiracaetano.dbank.payments.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class ApiContractIntegrationTest extends IntegrationTestBase {

    private static final Path CONTRACT = Path.of("..", "docs", "api", "payments-openapi.yaml");

    @Autowired
    MockMvc mockMvc;

    @Test
    void givenCurrentApi_whenComparingWithTheVersionedContract_thenShouldBeIdentical() throws Exception {
        var live = mockMvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        if (Boolean.getBoolean("updateContract")) {
            Files.createDirectories(CONTRACT.getParent());
            Files.writeString(CONTRACT, live);
        }

        assertThat(live)
                .as("The API contract changed; run ./gradlew integrationTest -PupdateContract and review the diff")
                .isEqualTo(Files.readString(CONTRACT));
    }
}
