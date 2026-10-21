package com.diegoferreiracaetano.dbank.payments.application.idempotency;

import com.diegoferreiracaetano.dbank.payments.domain.shared.Money;
import com.diegoferreiracaetano.dbank.payments.domain.transfer.AccountId;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public record RequestFingerprint(String value) {

    public static RequestFingerprint of(AccountId from, AccountId to, Money amount) {
        var canonical = from.value() + "|" + to.value() + "|" + amount.cents();
        return new RequestFingerprint(sha256(canonical));
    }

    private static String sha256(String text) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
