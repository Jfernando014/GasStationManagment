package com.edu.unicauca.gasstation.backend.sales.domain.calculation;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/** Exact intermediate arithmetic: no MathContext or rounded decimal division. */
public record ExactFraction(BigInteger numerator, BigInteger denominator) implements Comparable<ExactFraction> {
    public static final ExactFraction ZERO = of(BigDecimal.ZERO);
    public ExactFraction {
        Objects.requireNonNull(numerator); Objects.requireNonNull(denominator);
        if (denominator.signum() == 0) throw new IllegalArgumentException("Zero denominator");
        if (denominator.signum() < 0) { numerator = numerator.negate(); denominator = denominator.negate(); }
        var gcd = numerator.gcd(denominator);
        numerator = numerator.divide(gcd); denominator = denominator.divide(gcd);
    }
    public static ExactFraction of(BigDecimal value) {
        Objects.requireNonNull(value);
        // Normalize only this local arithmetic representation; the caller's immutable BigDecimal is untouched.
        if (value.signum() == 0) return new ExactFraction(BigInteger.ZERO, BigInteger.ONE);
        value = value.stripTrailingZeros();
        return value.scale() >= 0
                ? new ExactFraction(value.unscaledValue(), BigInteger.TEN.pow(value.scale()))
                : new ExactFraction(value.unscaledValue().multiply(BigInteger.TEN.pow(-value.scale())), BigInteger.ONE);
    }
    public ExactFraction add(ExactFraction other) {
        return new ExactFraction(numerator.multiply(other.denominator).add(other.numerator.multiply(denominator)),
                denominator.multiply(other.denominator));
    }
    public ExactFraction multiply(ExactFraction other) {
        return new ExactFraction(numerator.multiply(other.numerator), denominator.multiply(other.denominator));
    }
    public ExactFraction divide(ExactFraction other) {
        return new ExactFraction(numerator.multiply(other.denominator), denominator.multiply(other.numerator));
    }
    @Override public int compareTo(ExactFraction other) {
        return numerator.multiply(other.denominator).compareTo(other.numerator.multiply(denominator));
    }
}
