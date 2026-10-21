package com.diegoferreiracaetano.dbank.ledger.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AccountTest {

    @Test
    void shouldOpenWithZeroBalance() {
        assertThat(Account.open().balance()).isEqualTo(Money.ZERO);
    }

    @Test
    void shouldIncreaseBalanceOnCredit() {
        var account = Account.open().credit(Money.ofCents(500));

        assertThat(account.balance()).isEqualTo(Money.ofCents(500));
    }

    @Test
    void shouldDecreaseBalanceOnDebit() {
        var account = Account.open().credit(Money.ofCents(500)).debit(Money.ofCents(200));

        assertThat(account.balance()).isEqualTo(Money.ofCents(300));
    }

    @Test
    void shouldRejectDebitAboveBalance() {
        var account = Account.open().credit(Money.ofCents(100));

        assertThatThrownBy(() -> account.debit(Money.ofCents(101))).isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void shouldNotMutateOriginalAccountOnDebit() {
        var original = Account.open().credit(Money.ofCents(500));

        original.debit(Money.ofCents(200));

        assertThat(original.balance()).isEqualTo(Money.ofCents(500));
    }
}
