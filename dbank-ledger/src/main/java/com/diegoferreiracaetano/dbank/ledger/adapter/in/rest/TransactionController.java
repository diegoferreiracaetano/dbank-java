package com.diegoferreiracaetano.dbank.ledger.adapter.in.rest;

import com.diegoferreiracaetano.dbank.ledger.application.port.in.PostTransferUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/transactions")
class TransactionController {

    private final PostTransferUseCase postTransfer;

    TransactionController(PostTransferUseCase postTransfer) {
        this.postTransfer = postTransfer;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void post(@Valid @RequestBody PostTransactionRequest request) {
        postTransfer.post(request.toCommand());
    }
}
