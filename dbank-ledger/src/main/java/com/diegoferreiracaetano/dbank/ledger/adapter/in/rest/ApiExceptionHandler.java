package com.diegoferreiracaetano.dbank.ledger.adapter.in.rest;

import com.diegoferreiracaetano.dbank.ledger.application.port.out.ConcurrentUpdateException;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountNotFoundException;
import com.diegoferreiracaetano.dbank.ledger.domain.account.InsufficientFundsException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    ProblemDetail accountNotFound(AccountNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "account-not-found", "Account not found", e.getMessage());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    ProblemDetail insufficientFunds(InsufficientFundsException e) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "insufficient-funds", "Insufficient funds", e.getMessage());
    }

    @ExceptionHandler(ConcurrentUpdateException.class)
    ProblemDetail concurrentUpdate(ConcurrentUpdateException e) {
        return problem(HttpStatus.CONFLICT, "concurrent-update", "Concurrent update", e.getMessage());
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
