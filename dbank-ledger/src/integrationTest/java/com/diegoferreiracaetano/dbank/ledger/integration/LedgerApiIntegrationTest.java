package com.diegoferreiracaetano.dbank.ledger.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class LedgerApiIntegrationTest extends IntegrationTestBase {

    private static final String TREASURY = "00000000-0000-0000-0000-000000000001";

    @Autowired
    MockMvc mockMvc;

    @Test
    void givenTreasuryFunding_whenTransferringBetweenAccounts_thenShouldReflectBalancesAndStatement() throws Exception {
        var a = openAccount();
        var b = openAccount();

        postTransaction(UUID.randomUUID(), TREASURY, a, 1000).andExpect(status().isNoContent());
        postTransaction(UUID.randomUUID(), a, b, 300).andExpect(status().isNoContent());

        assertThat(balanceOf(a)).isEqualTo(700);
        assertThat(balanceOf(b)).isEqualTo(300);
        assertThat(statementAmounts(a)).containsExactlyInAnyOrder(300, 1000);
        assertThat(statementAmounts(b)).containsExactly(300);
    }

    @Test
    void givenSameTransactionIdTwice_whenPosting_thenBalancesShouldStayTheSame() throws Exception {
        var a = openAccount();
        var b = openAccount();
        postTransaction(UUID.randomUUID(), TREASURY, a, 1000).andExpect(status().isNoContent());
        var transactionId = UUID.randomUUID();
        postTransaction(transactionId, a, b, 300).andExpect(status().isNoContent());

        postTransaction(transactionId, a, b, 300).andExpect(status().isNoContent());

        assertThat(balanceOf(a)).isEqualTo(700);
        assertThat(balanceOf(b)).isEqualTo(300);
        assertThat(statementAmounts(b)).containsExactly(300);
    }

    @Test
    void givenInsufficientFunds_whenPosting_thenShouldReturnUnprocessableEntityAndKeepBalances() throws Exception {
        var a = openAccount();
        var b = openAccount();

        postTransaction(UUID.randomUUID(), a, b, 500).andExpect(status().isUnprocessableEntity());

        assertThat(balanceOf(a)).isZero();
        assertThat(balanceOf(b)).isZero();
        assertThat(statementAmounts(a)).isEmpty();
    }

    @Test
    void givenUnknownAccount_whenGetting_thenShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/v1/accounts/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }

    private String openAccount() throws Exception {
        var body = mockMvc.perform(post("/v1/accounts"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private org.springframework.test.web.servlet.ResultActions postTransaction(
            UUID transactionId, String from, String to, long amountCents) throws Exception {
        var body = """
                {"transactionId":"%s","fromAccountId":"%s","toAccountId":"%s","amountCents":%d}
                """.formatted(transactionId, from, to, amountCents);
        return mockMvc.perform(
                post("/v1/transactions").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private int balanceOf(String accountId) throws Exception {
        var body = mockMvc.perform(get("/v1/accounts/" + accountId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.balanceCents");
    }

    private List<Integer> statementAmounts(String accountId) throws Exception {
        var body = mockMvc.perform(get("/v1/accounts/" + accountId + "/entries"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$[*].amountCents");
    }
}
