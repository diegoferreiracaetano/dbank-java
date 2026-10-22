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
        responseCode = "404",
        description = "The transfer does not exist",
        content =
                @Content(
                        mediaType = "application/problem+json",
                        schema = @Schema(implementation = ProblemDetail.class),
                        examples = @ExampleObject(value = """
                                                {"type":"urn:dbank:problem:transfer-not-found","title":"Transfer not found","status":404,"detail":"Transfer not found: 6f1c0a52-3d3e-4c1f-9d4b-2a7f6e0b9a11"}
                                                """)))
@interface NotFoundProblem {}
