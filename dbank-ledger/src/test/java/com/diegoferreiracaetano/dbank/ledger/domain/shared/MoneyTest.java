package com.diegoferreiracaetano.dbank.ledger.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void shouldRejectNegativeAmount() {
        assertThatThrownBy(() -> Money.ofCents(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAddExactlyWithoutFloatingPointError() {
        var result = Money.ofCents(10).plus(Money.ofCents(20));

        assertThat(result).isEqualTo(Money.ofCents(30));
    }

    @Test
    void shouldFailOnOverflow() {
        assertThatThrownBy(() -> Money.ofCents(Long.MAX_VALUE).plus(Money.ofCents(1)))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void shouldCompareAmounts() {
        assertThat(Money.ofCents(100).isGreaterThanOrEqualTo(Money.ofCents(100)))
                .isTrue();
        assertThat(Money.ofCents(99).isGreaterThanOrEqualTo(Money.ofCents(100))).isFalse();
    }
}
