package com.diegoferreiracaetano.dbank.payments.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

interface TransferApi {

    @Operation(
            summary = "Request a transfer",
            description =
                    "Accepts a transfer for asynchronous processing. Idempotent by the Idempotency-Key header: repeating the same key with the same body returns the original transfer.")
    @Parameter(
            name = "Idempotency-Key",
            in = ParameterIn.HEADER,
            required = true,
            description = "Client-generated key (up to 255 characters). Reuse it on retries of the same intention.")
    @ApiResponse(
            responseCode = "202",
            description = "Accepted; the transfer is PENDING until the ledger answers",
            headers = {
                @Header(name = "Location", description = "Path of the transfer", schema = @Schema(type = "string")),
                @Header(
                        name = "Idempotency-Replayed",
                        description = "true when this response replays a previous request",
                        schema = @Schema(type = "boolean"))
            })
    @BadRequestProblem
    @IdempotencyKeyReuseProblem
    ResponseEntity<TransferResponse> request(String idempotencyKey, @Valid RequestTransferRequest request);

    @Operation(summary = "Get a transfer", description = "Returns the current state of the transfer.")
    @ApiResponse(responseCode = "200", description = "The transfer")
    @BadRequestProblem
    @NotFoundProblem
    TransferResponse get(UUID id);
}
