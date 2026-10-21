package com.diegoferreiracaetano.dbank.ledger.adapter.in.rest;

import com.diegoferreiracaetano.dbank.ledger.application.port.in.GetAccountUseCase;
import com.diegoferreiracaetano.dbank.ledger.application.port.in.GetStatementUseCase;
import com.diegoferreiracaetano.dbank.ledger.application.port.in.OpenAccountUseCase;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/v1/accounts", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Accounts")
class AccountController implements AccountApi {

    private final OpenAccountUseCase openAccount;
    private final GetAccountUseCase getAccount;
    private final GetStatementUseCase getStatement;

    AccountController(OpenAccountUseCase openAccount, GetAccountUseCase getAccount, GetStatementUseCase getStatement) {
        this.openAccount = openAccount;
        this.getAccount = getAccount;
        this.getStatement = getStatement;
    }

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<AccountResponse> open() {
        var account = openAccount.open();
        return ResponseEntity.created(URI.create("/v1/accounts/" + account.id().value()))
                .body(AccountResponse.from(account));
    }

    @Override
    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable UUID id) {
        return AccountResponse.from(getAccount.get(new AccountId(id)));
    }

    @Override
    @GetMapping("/{id}/entries")
    public List<EntryResponse> entries(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return getStatement.get(new AccountId(id), page, size).stream()
                .map(EntryResponse::from)
                .toList();
    }
}
