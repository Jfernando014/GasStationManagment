package com.edu.unicauca.gasstation.backend.sales.domain.calculation;

import java.math.*;
import java.util.*;

/** Explicit largest-remainder policy. Ties follow input order; outputs conserve the input exactly. */
public final class ConservingAllocation {
    private ConservingAllocation() { }
    public static List<BigDecimal> allocate(BigDecimal total, List<ExactFraction> weights, int scale) {
        Objects.requireNonNull(total); Objects.requireNonNull(weights);
        if (scale < 0 || scale > 12) throw new IllegalArgumentException("Output scale must be 0..12");
        if (total.signum() < 0) throw new IllegalArgumentException("Negative total");
        final BigInteger units;
        try { units = total.setScale(scale, RoundingMode.UNNECESSARY).unscaledValue(); }
        catch (ArithmeticException ex) { throw new IllegalArgumentException("Target is not exact at requested output scale", ex); }
        var sum = ExactFraction.ZERO;
        for (var weight : weights) {
            Objects.requireNonNull(weight);
            if (weight.compareTo(ExactFraction.ZERO) < 0) throw new IllegalArgumentException("Negative weight");
            sum = sum.add(weight);
        }
        if (sum.compareTo(ExactFraction.ZERO) <= 0) throw new IllegalArgumentException("Positive weight sum required");
        var amounts = new ArrayList<BigInteger>();
        var remainders = new ArrayList<ExactFraction>();
        BigInteger allocated = BigInteger.ZERO;
        for (var weight : weights) {
            var quota = new ExactFraction(units, BigInteger.ONE).multiply(weight).divide(sum);
            var parts = quota.numerator().divideAndRemainder(quota.denominator());
            amounts.add(parts[0]); allocated = allocated.add(parts[0]);
            remainders.add(new ExactFraction(parts[1], quota.denominator()));
        }
        var order = new ArrayList<Integer>();
        for (int i = 0; i < weights.size(); i++) order.add(i);
        order.sort((a, b) -> { int cmp = remainders.get(b).compareTo(remainders.get(a)); return cmp == 0 ? Integer.compare(a, b) : cmp; });
        int residual = units.subtract(allocated).intValueExact();
        for (int i = 0; i < residual; i++) {
            int index = order.get(i); amounts.set(index, amounts.get(index).add(BigInteger.ONE));
        }
        return amounts.stream().map(value -> new BigDecimal(value, scale)).toList();
    }
}
