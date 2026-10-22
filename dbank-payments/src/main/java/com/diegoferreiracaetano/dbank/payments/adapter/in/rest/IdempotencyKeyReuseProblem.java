package com.diegoferreiracaetano.dbank.payments.adapter.in.rest;

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
        description = "The Idempotency-Key was already used with a different request",
        content =
                @Content(
                        mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class),
                        examples = @ExampleObject(value = """
                                                {"type":"urn:dbank:problem:idempotency-key-reuse","title":"Idempotency key reuse","status":422,"detail":"Idempotency key already used with a different request: 7d1c"}
                                                """)))
@interface IdempotencyKeyReuseProblem {}
