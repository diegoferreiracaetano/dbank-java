package com.diegoferreiracaetano.dbank.ledger.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegoferreiracaetano.dbank.ledger.application.port.in.GetAccountUseCase;
import com.diegoferreiracaetano.dbank.ledger.application.port.in.GetStatementUseCase;
import com.diegoferreiracaetano.dbank.ledger.application.port.in.OpenAccountUseCase;
import com.diegoferreiracaetano.dbank.ledger.domain.account.Account;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountNotFoundException;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerEntry;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransactionId;
import com.diegoferreiracaetano.dbank.ledger.domain.shared.Money;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OpenAccountUseCase openAccount;

    @MockitoBean
    GetAccountUseCase getAccount;

    @MockitoBean
    GetStatementUseCase getStatement;

    @Test
    void givenNewAccount_whenOpening_thenShouldReturnCreatedWithLocationAndZeroBalance() throws Exception {
        var account = Account.open();
        when(openAccount.open()).thenReturn(account);

        mockMvc.perform(post("/v1/accounts"))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                                "Location", "/v1/accounts/" + account.id().value()))
                .andExpect(jsonPath("$.id").value(account.id().value().toString()))
                .andExpect(jsonPath("$.balanceCents").value(0));
    }

    @Test
    void givenExistingAccount_whenGetting_thenShouldReturnBalance() throws Exception {
        var account = new Account(AccountId.generate(), Money.ofCents(750));
        when(getAccount.get(account.id())).thenReturn(account);

        mockMvc.perform(get("/v1/accounts/" + account.id().value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceCents").value(750));
    }

    @Test
    void givenUnknownAccount_whenGetting_thenShouldReturnNotFoundProblem() throws Exception {
        var id = AccountId.generate();
        when(getAccount.get(any())).thenThrow(new AccountNotFoundException(id));

        mockMvc.perform(get("/v1/accounts/" + id.value()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type").value("urn:dbank:problem:account-not-found"));
    }

    @Test
    void givenEntries_whenGettingStatement_thenShouldReturnThemWithDirectionAndAmount() throws Exception {
        var id = AccountId.generate();
        var entry = LedgerEntry.credit(
                LedgerTransactionId.generate(), id, Money.ofCents(100), Instant.parse("2026-01-01T00:00:00Z"));
        when(getStatement.get(eq(id), eq(0), eq(20))).thenReturn(List.of(entry));

        mockMvc.perform(get("/v1/accounts/" + id.value() + "/entries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].direction").value("CREDIT"))
                .andExpect(jsonPath("$[0].amountCents").value(100));
    }

    @Test
    void givenSizeAboveLimit_whenGettingStatement_thenShouldReturnBadRequestProblem() throws Exception {
        var id = AccountId.generate();

        mockMvc.perform(get("/v1/accounts/" + id.value() + "/entries?size=1000"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }
}
