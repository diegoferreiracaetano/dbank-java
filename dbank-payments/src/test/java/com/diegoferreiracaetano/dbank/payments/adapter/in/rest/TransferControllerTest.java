package com.diegoferreiracaetano.dbank.payments.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyKey;
import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyKeyReuseException;
import com.diegoferreiracaetano.dbank.payments.application.port.in.GetTransferUseCase;
import com.diegoferreiracaetano.dbank.payments.application.port.in.RequestTransferResult;
import com.diegoferreiracaetano.dbank.payments.application.port.in.RequestTransferUseCase;
import com.diegoferreiracaetano.dbank.payments.domain.shared.Money;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.AccountId;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.Transfer;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferId;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferNotFoundException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TransferController.class)
class TransferControllerTest {

    private static final String BODY = """
            {"fromAccountId":"%s","toAccountId":"%s","amountCents":%d}
            """;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RequestTransferUseCase requestTransfer;

    @MockitoBean
    GetTransferUseCase getTransfer;

    @Test
    void givenNewRequest_whenPosting_thenShouldReturnAcceptedWithLocationAndNoReplayHeader() throws Exception {
        var transfer = pending();
        when(requestTransfer.request(any())).thenReturn(new RequestTransferResult(transfer, false));

        mockMvc.perform(post("/v1/transfers")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(100)))
                .andExpect(status().isAccepted())
                .andExpect(header().string(
                                "Location", "/v1/transfers/" + transfer.id().value()))
                .andExpect(header().doesNotExist("Idempotency-Replayed"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void givenReplay_whenPosting_thenShouldAddTheReplayedHeader() throws Exception {
        when(requestTransfer.request(any())).thenReturn(new RequestTransferResult(pending(), true));

        mockMvc.perform(post("/v1/transfers")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(100)))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Idempotency-Replayed", "true"));
    }

    @Test
    void givenMissingKey_whenPosting_thenShouldReturnBadRequestProblem() throws Exception {
        mockMvc.perform(post("/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(100)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void givenZeroAmount_whenPosting_thenShouldReturnBadRequestProblem() throws Exception {
        mockMvc.perform(post("/v1/transfers")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(0)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void givenKeyReuse_whenPosting_thenShouldReturnUnprocessableEntityProblem() throws Exception {
        when(requestTransfer.request(any())).thenThrow(new IdempotencyKeyReuseException(new IdempotencyKey("key-1")));

        mockMvc.perform(post("/v1/transfers")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(100)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("urn:dbank:problem:idempotency-key-reuse"));
    }

    @Test
    void givenExistingTransfer_whenGetting_thenShouldReturnIt() throws Exception {
        var transfer = pending();
        when(getTransfer.get(transfer.id())).thenReturn(transfer);

        mockMvc.perform(get("/v1/transfers/" + transfer.id().value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amountCents").value(100))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void givenUnknownTransfer_whenGetting_thenShouldReturnNotFoundProblem() throws Exception {
        var id = TransferId.generate();
        when(getTransfer.get(any())).thenThrow(new TransferNotFoundException(id));

        mockMvc.perform(get("/v1/transfers/" + id.value()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type").value("urn:dbank:problem:transfer-not-found"));
    }

    private static String body(long amountCents) {
        return BODY.formatted(UUID.randomUUID(), UUID.randomUUID(), amountCents);
    }

    private static Transfer pending() {
        return Transfer.request(
                TransferId.generate(),
                AccountId.generate(),
                AccountId.generate(),
                Money.ofCents(100),
                Instant.parse("2026-01-01T00:00:00Z"));
    }
}
