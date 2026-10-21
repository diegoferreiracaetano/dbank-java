package com.diegoferreiracaetano.dbank.payments.domain.shared;

public record Money(long cents) {

    public static final Money ZERO = new Money(0);

    public Money {
        if (cents < 0) {
            throw new IllegalArgumentException("Money cannot be negative: " + cents);
        }
    }

    public static Money ofCents(long cents) {
        return new Money(cents);
    }

    public boolean isZero() {
        return cents == 0;
    }
}
