package com.diegoferreiracaetano.dbank.ledger.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.diegoferreiracaetano.dbank.ledger.domain.account.Account;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountNotFoundException;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerEntry;
import com.diegoferreiracaetano.dbank.ledger.domain.ledger.LedgerTransactionId;
import com.diegoferreiracaetano.dbank.ledger.domain.shared.Money;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetStatementServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();

    @Test
    void givenUnknownAccount_whenGettingStatement_thenShouldThrowAccountNotFound() {
        var service = new GetStatementService(accounts, (id, page, size) -> List.of());
        var id = AccountId.generate();

        assertThatThrownBy(() -> service.get(id, 0, 20)).isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void givenKnownAccount_whenGettingStatement_thenShouldReturnRepositoryEntries() {
        var account = Account.open();
        accounts.save(account);
        var entry = LedgerEntry.credit(LedgerTransactionId.generate(), account.id(), Money.ofCents(100), NOW);
        var service = new GetStatementService(accounts, (id, page, size) -> List.of(entry));

        var result = service.get(account.id(), 0, 20);

        assertThat(result).containsExactly(entry);
    }

    @Test
    void givenKnownAccount_whenGettingStatement_thenShouldPassPagingToRepository() {
        var account = Account.open();
        accounts.save(account);
        var received = new int[2];
        var service = new GetStatementService(accounts, (id, page, size) -> {
            received[0] = page;
            received[1] = size;
            return List.of();
        });

        service.get(account.id(), 3, 50);

        assertThat(received).containsExactly(3, 50);
    }
}
