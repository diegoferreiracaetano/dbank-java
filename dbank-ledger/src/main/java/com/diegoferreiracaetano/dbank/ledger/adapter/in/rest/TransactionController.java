package com.diegoferreiracaetano.dbank.ledger.adapter.in.rest;

import com.diegoferreiracaetano.dbank.ledger.application.port.in.PostTransferUseCase;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/transactions")
@Tag(name = "Transactions")
class TransactionController implements TransactionApi {

    private final PostTransferUseCase postTransfer;

    TransactionController(PostTransferUseCase postTransfer) {
        this.postTransfer = postTransfer;
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void post(@RequestBody PostTransactionRequest request) {
        postTransfer.post(request.toCommand());
    }
}
