package com.diegoferreiracaetano.dbank.payments.adapter.in.rest;

import com.diegoferreiracaetano.dbank.payments.application.port.in.GetTransferUseCase;
import com.diegoferreiracaetano.dbank.payments.application.port.in.RequestTransferUseCase;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.TransferId;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/v1/transfers", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Transfers")
class TransferController implements TransferApi {

    private static final String REPLAYED_HEADER = "Idempotency-Replayed";

    private final RequestTransferUseCase requestTransfer;
    private final GetTransferUseCase getTransfer;

    TransferController(RequestTransferUseCase requestTransfer, GetTransferUseCase getTransfer) {
        this.requestTransfer = requestTransfer;
        this.getTransfer = getTransfer;
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<TransferResponse> request(
            @RequestHeader("Idempotency-Key") String idempotencyKey, @RequestBody RequestTransferRequest request) {
        var result = requestTransfer.request(request.toCommand(idempotencyKey));
        var response = ResponseEntity.accepted()
                .location(URI.create("/v1/transfers/" + result.transfer().id().value()));
        if (result.replayed()) {
            response.header(REPLAYED_HEADER, "true");
        }
        return response.body(TransferResponse.from(result.transfer()));
    }

    @Override
    @GetMapping("/{id}")
    public TransferResponse get(@PathVariable UUID id) {
        return TransferResponse.from(getTransfer.get(new TransferId(id)));
    }
}
