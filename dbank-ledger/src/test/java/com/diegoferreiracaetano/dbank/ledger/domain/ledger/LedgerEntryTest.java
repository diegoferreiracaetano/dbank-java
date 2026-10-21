package com.diegoferreiracaetano.dbank.ledger.domain.ledger;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.diegoferreiracaetano.dbank.ledger.domain.account.AccountId;
import com.diegoferreiracaetano.dbank.ledger.domain.shared.Money;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class LedgerEntryTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void givenZeroAmount_whenCreatingDebit_thenShouldReject() {
        var transactionId = LedgerTransactionId.generate();
        var accountId = AccountId.generate();

        assertThatThrownBy(() -> LedgerEntry.debit(transactionId, accountId, Money.ZERO, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenZeroAmount_whenCreatingCredit_thenShouldReject() {
        var transactionId = LedgerTransactionId.generate();
        var accountId = AccountId.generate();

        assertThatThrownBy(() -> LedgerEntry.credit(transactionId, accountId, Money.ZERO, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
