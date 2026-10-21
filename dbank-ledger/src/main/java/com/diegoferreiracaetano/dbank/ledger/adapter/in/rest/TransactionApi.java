package com.diegoferreiracaetano.dbank.ledger.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;

interface TransactionApi {

    @Operation(
            summary = "Post a transaction",
            description =
                    "Debits one account and credits another atomically. Idempotent by transactionId: reposting a posted id changes nothing.")
    @ApiResponse(responseCode = "204", description = "Posted, or already posted with the same transactionId")
    @BadRequestProblem
    @NotFoundProblem
    @ConflictProblem
    @UnprocessableProblem
    void post(@Valid PostTransactionRequest request);
}
