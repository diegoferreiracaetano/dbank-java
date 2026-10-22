package com.diegoferreiracaetano.dbank.payments.adapter.in.rest;

import com.diegoferreiracaetano.dbank.payments.application.idempotency.IdempotencyKeyReuseException;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferNotFoundException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(TransferNotFoundException.class)
    ProblemDetail transferNotFound(TransferNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "transfer-not-found", "Transfer not found", e.getMessage());
    }

    @ExceptionHandler(IdempotencyKeyReuseException.class)
    ProblemDetail idempotencyKeyReuse(IdempotencyKeyReuseException e) {
        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY, "idempotency-key-reuse", "Idempotency key reuse", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidArgument(IllegalArgumentException e) {
        return problem(HttpStatus.BAD_REQUEST, "invalid-request", "Invalid request", e.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String type, String title, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("urn:dbank:problem:" + type));
        problem.setTitle(title);
        return problem;
    }
}
