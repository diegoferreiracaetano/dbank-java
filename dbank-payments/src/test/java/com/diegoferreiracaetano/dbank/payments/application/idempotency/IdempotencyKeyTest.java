package com.diegoferreiracaetano.dbank.payments.application.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IdempotencyKeyTest {

    @Test
    void givenNullValue_whenCreating_thenShouldReject() {
        assertThatThrownBy(() -> new IdempotencyKey(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenBlankValue_whenCreating_thenShouldReject() {
        assertThatThrownBy(() -> new IdempotencyKey("   ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenValueAboveTheLimit_whenCreating_thenShouldReject() {
        var tooLong = "a".repeat(256);

        assertThatThrownBy(() -> new IdempotencyKey(tooLong)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenValueAtTheLimit_whenCreating_thenShouldAccept() {
        var atTheLimit = "a".repeat(255);

        assertThat(new IdempotencyKey(atTheLimit).value()).isEqualTo(atTheLimit);
    }
}
