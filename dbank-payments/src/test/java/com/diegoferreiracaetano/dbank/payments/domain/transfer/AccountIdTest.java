package com.diegoferreiracaetano.dbank.payments.domain.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AccountIdTest {

    @Test
    void givenNullValue_whenCreating_thenShouldReject() {
        assertThatThrownBy(() -> new AccountId(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenTwoGeneratedIds_whenComparing_thenShouldBeDifferent() {
        assertThat(AccountId.generate()).isNotEqualTo(AccountId.generate());
    }
}
