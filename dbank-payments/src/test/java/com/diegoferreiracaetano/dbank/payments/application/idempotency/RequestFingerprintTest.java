package com.diegoferreiracaetano.dbank.payments.application.idempotency;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegoferreiracaetano.dbank.payments.domain.shared.Money;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.AccountId;
import org.junit.jupiter.api.Test;

class RequestFingerprintTest {

    private final AccountId from = AccountId.generate();
    private final AccountId to = AccountId.generate();

    @Test
    void givenSameFields_whenFingerprinting_thenShouldBeEqual() {
        var first = RequestFingerprint.of(from, to, Money.ofCents(100));
        var second = RequestFingerprint.of(from, to, Money.ofCents(100));

        assertThat(first).isEqualTo(second);
    }

    @Test
    void givenDifferentAmount_whenFingerprinting_thenShouldDiffer() {
        var first = RequestFingerprint.of(from, to, Money.ofCents(100));
        var second = RequestFingerprint.of(from, to, Money.ofCents(101));

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void givenSwappedAccounts_whenFingerprinting_thenShouldDiffer() {
        var first = RequestFingerprint.of(from, to, Money.ofCents(100));
        var swapped = RequestFingerprint.of(to, from, Money.ofCents(100));

        assertThat(first).isNotEqualTo(swapped);
    }

    @Test
    void givenAnyFields_whenFingerprinting_thenShouldProduceA64CharacterHexDigest() {
        var fingerprint = RequestFingerprint.of(from, to, Money.ofCents(100));

        assertThat(fingerprint.value()).matches("[0-9a-f]{64}");
    }
}
