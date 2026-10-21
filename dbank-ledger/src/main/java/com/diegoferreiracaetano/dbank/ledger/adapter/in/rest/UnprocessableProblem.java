package com.diegoferreiracaetano.dbank.ledger.adapter.in.rest;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.http.ProblemDetail;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponse(
        responseCode = "422",
        description = "A business rule prevents the operation, such as insufficient funds",
        content =
                @Content(
                        mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class),
                        examples = @ExampleObject(value = """
                                                {"type":"urn:dbank:problem:insufficient-funds","title":"Insufficient funds","status":422,"detail":"Account 6f1c0a52-3d3e-4c1f-9d4b-2a7f6e0b9a11 has 100 cents but 300 were requested"}
                                                """)))
@interface UnprocessableProblem {}
