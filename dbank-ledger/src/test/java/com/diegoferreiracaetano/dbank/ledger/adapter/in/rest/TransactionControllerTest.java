package com.diegoferreiracaetano.dbank.ledger.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegoferreiracaetano.dbank.ledger.application.port.in.PostTransferUseCase;
import com.diegoferreiracaetano.dbank.ledger.application.port.out.ConcurrentUpdateException;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.account.InsufficientFundsException;
import com.diegoferreiracaetano.dbank.ledger.domain.shared.Money;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    private static final String BODY = """
            {"transactionId":"%s","fromAccountId":"%s","toAccountId":"%s","amountCents":%d}
            """;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PostTransferUseCase postTransfer;

    @Test
    void givenValidRequest_whenPosting_thenShouldReturnNoContentAndCallUseCase() throws Exception {
        var body = body(300);

        mockMvc.perform(post("/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        verify(postTransfer).post(any());
    }

    @Test
    void givenZeroAmount_whenPosting_thenShouldReturnBadRequestProblem() throws Exception {
        var body = body(0);

        mockMvc.perform(post("/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void givenInsufficientFunds_whenPosting_thenShouldReturnUnprocessableEntityProblem() throws Exception {
        doThrow(new InsufficientFundsException(AccountId.generate(), Money.ofCents(1), Money.ofCents(2)))
                .when(postTransfer)
                .post(any());
        var body = body(300);

        mockMvc.perform(post("/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("urn:dbank:problem:insufficient-funds"));
    }

    @Test
    void givenConcurrentUpdate_whenPosting_thenShouldReturnConflictProblem() throws Exception {
        doThrow(new ConcurrentUpdateException(new RuntimeException()))
                .when(postTransfer)
                .post(any());
        var body = body(300);

        mockMvc.perform(post("/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:dbank:problem:concurrent-update"));
    }

    @Test
    void givenSameAccountOnBothSides_whenPosting_thenShouldReturnBadRequestProblem() throws Exception {
        doThrow(new IllegalArgumentException("Cannot transfer to the same account"))
                .when(postTransfer)
                .post(any());
        var body = body(300);

        mockMvc.perform(post("/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:dbank:problem:invalid-request"));
    }

    private static String body(long amountCents) {
        return BODY.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), amountCents);
    }
}
