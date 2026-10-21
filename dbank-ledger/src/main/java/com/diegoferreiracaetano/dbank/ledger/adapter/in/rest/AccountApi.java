package com.diegoferreiracaetano.dbank.ledger.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

interface AccountApi {

    @Operation(summary = "Open an account", description = "Creates an account with a zero balance.")
    @ApiResponse(
            responseCode = "201",
            description = "Account opened",
            headers =
                    @Header(
                            name = "Location",
                            description = "Path of the new account",
                            schema = @Schema(type = "string")))
    ResponseEntity<AccountResponse> open();

    @Operation(summary = "Get an account", description = "Returns the account and its current balance in cents.")
    @ApiResponse(responseCode = "200", description = "The account")
    @BadRequestProblem
    @NotFoundProblem
    AccountResponse get(UUID id);

    @Operation(
            summary = "Get the account statement",
            description = "Ledger entries of the account, newest first. page is zero-based; size defaults to 20.")
    @ApiResponse(responseCode = "200", description = "A page of entries, possibly empty")
    @BadRequestProblem
    @NotFoundProblem
    List<EntryResponse> entries(UUID id, @Min(0) int page, @Min(1) @Max(100) int size);
}
