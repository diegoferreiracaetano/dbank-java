package com.diegoferreiracaetano.dbank.payments.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void givenNegativeCents_whenCreating_thenShouldReject() {
        assertThatThrownBy(() -> Money.ofCents(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenZeroCents_whenCheckingIfZero_thenShouldBeTrue() {
        assertThat(Money.ofCents(0).isZero()).isTrue();
    }

    @Test
    void givenPositiveCents_whenCheckingIfZero_thenShouldBeFalse() {
        assertThat(Money.ofCents(1).isZero()).isFalse();
    }
}
