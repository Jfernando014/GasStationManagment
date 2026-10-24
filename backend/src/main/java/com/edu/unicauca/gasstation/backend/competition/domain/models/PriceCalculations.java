package com.edu.unicauca.gasstation.backend.competition.domain.models;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Derived prices and percentages: six decimal places, HALF_EVEN. No scenario persistence. */
public final class PriceCalculations {
    private PriceCalculations() { }
    public static final int SCALE = 6;
    public record Difference(BigDecimal absoluteCopPerGallon, BigDecimal percentage) { }
    public record Margin(BigDecimal grossCopPerGallon, BigDecimal grossMarginPercentage) { }
    public record Summary(int competitorCount, BigDecimal average, BigDecimal minimum) { }

    public static void money(BigDecimal value, boolean allowZero) {
        if (value == null || value.signum() < 0 || (!allowZero && value.signum() == 0)
                || value.compareTo(new BigDecimal("10000000000")) >= 0)
            throw new IllegalArgumentException("Amount must be " + (allowZero ? "nonnegative" : "positive") + " and below 10^10 COP/gallon");
        try { value.setScale(2, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException ex) { throw new IllegalArgumentException("Amount requires more than two decimal places"); }
    }

    public static Summary summarize(List<PriceObservation> latest) {
        if (latest.isEmpty()) return new Summary(0, null, null);
        var values = latest.stream().map(PriceObservation::pricePerGallon).toList();
        var total = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Summary(values.size(), total.divide(BigDecimal.valueOf(values.size()), SCALE, RoundingMode.HALF_EVEN),
                values.stream().min(BigDecimal::compareTo).orElseThrow());
    }

    /** Positive means Bellavista/hypothetical price is above the competitor reference. */
    public static Difference difference(BigDecimal price, BigDecimal reference) {
        if (price == null || reference == null) return new Difference(null, null);
        var delta = price.subtract(reference);
        return new Difference(delta.setScale(SCALE, RoundingMode.HALF_EVEN), reference.signum() == 0 ? null
                : delta.multiply(BigDecimal.valueOf(100)).divide(reference, SCALE, RoundingMode.HALF_EVEN));
    }

    /** Gross margin on selling price; cost is explicitly supplied, not inferred from history. */
    public static Margin margin(BigDecimal price, BigDecimal cost) {
        money(price, false);
        if (cost == null) return new Margin(null, null);
        money(cost, true);
        var gross = price.subtract(cost);
        return new Margin(gross.setScale(2, RoundingMode.UNNECESSARY),
                gross.multiply(BigDecimal.valueOf(100)).divide(price, SCALE, RoundingMode.HALF_EVEN));
    }
}
