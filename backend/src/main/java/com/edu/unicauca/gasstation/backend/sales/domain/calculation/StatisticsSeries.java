package com.edu.unicauca.gasstation.backend.sales.domain.calculation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/** Only observed buckets are emitted. Changes compare consecutive observed buckets, never padded dates. */
public final class StatisticsSeries {
    private StatisticsSeries() { }
    public record Value(LocalDate date, BigDecimal value) { }
    public record Bucket(LocalDate start, BigDecimal total, int observations, BigDecimal average,
                         BigDecimal change, BigDecimal changePercent) { }

    public static List<Bucket> aggregate(List<Value> values, StatisticsPeriod period, int scale) {
        Objects.requireNonNull(period, "period");
        if (scale < 0 || scale > 12) throw new IllegalArgumentException("outputScale must be 0..12");
        var grouped = new TreeMap<LocalDate, List<Value>>();
        for (var value : values) {
            if (value.value().signum() < 0) throw new IllegalArgumentException("Negative observations are not supported");
            grouped.computeIfAbsent(period.start(value.date()), ignored -> new ArrayList<>()).add(value);
        }
        var result = new ArrayList<Bucket>();
        BigDecimal previous = null;
        for (var entry : grouped.entrySet()) {
            var total = entry.getValue().stream().map(Value::value).reduce(BigDecimal.ZERO, BigDecimal::add);
            var change = previous == null ? null : total.subtract(previous);
            var percent = previous == null || previous.signum() == 0 ? null
                    : change.multiply(BigDecimal.valueOf(100)).divide(previous, scale, RoundingMode.HALF_EVEN);
            result.add(new Bucket(entry.getKey(), total, entry.getValue().size(),
                    total.divide(BigDecimal.valueOf(entry.getValue().size()), scale, RoundingMode.HALF_EVEN), change, percent));
            previous = total;
        }
        return List.copyOf(result);
    }
}
