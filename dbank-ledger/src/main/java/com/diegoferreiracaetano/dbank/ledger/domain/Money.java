package com.diegoferreiracaetano.dbank.ledger.domain;

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

    public Money plus(Money other) {
        return new Money(Math.addExact(cents, other.cents));
    }

    public Money minus(Money other) {
        return new Money(cents - other.cents);
    }

    public boolean isGreaterThanOrEqualTo(Money other) {
        return cents >= other.cents;
    }
}
