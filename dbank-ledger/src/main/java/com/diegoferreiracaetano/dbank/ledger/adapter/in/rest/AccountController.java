package com.diegoferreiracaetano.dbank.ledger.adapter.in.rest;

import com.diegoferreiracaetano.dbank.ledger.application.port.in.GetAccountUseCase;
import com.diegoferreiracaetano.dbank.ledger.application.port.in.GetStatementUseCase;
import com.diegoferreiracaetano.dbank.ledger.application.port.in.OpenAccountUseCase;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/accounts")
class AccountController {

    private final OpenAccountUseCase openAccount;
    private final GetAccountUseCase getAccount;
    private final GetStatementUseCase getStatement;

    AccountController(OpenAccountUseCase openAccount, GetAccountUseCase getAccount, GetStatementUseCase getStatement) {
        this.openAccount = openAccount;
        this.getAccount = getAccount;
        this.getStatement = getStatement;
    }

    @PostMapping
    ResponseEntity<AccountResponse> open() {
        var account = openAccount.open();
        return ResponseEntity.created(URI.create("/v1/accounts/" + account.id().value()))
                .body(AccountResponse.from(account));
    }

    @GetMapping("/{id}")
    AccountResponse get(@PathVariable UUID id) {
        return AccountResponse.from(getAccount.get(new AccountId(id)));
    }

    @GetMapping("/{id}/entries")
    List<EntryResponse> entries(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return getStatement.get(new AccountId(id), page, size).stream()
                .map(EntryResponse::from)
                .toList();
    }
}
