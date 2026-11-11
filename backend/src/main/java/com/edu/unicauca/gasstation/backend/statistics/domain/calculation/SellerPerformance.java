package com.edu.unicauca.gasstation.backend.statistics.domain.calculation;

import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistory;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistoryRange;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleIdentity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Internal SCRUM-106 inputs/results. No public sales/shifts contract or production default is introduced. */
public final class SellerPerformance {
    private SellerPerformance() { }

    public enum Availability { AVAILABLE, PARTIAL, UNAVAILABLE, NO_DATA }
    public enum Reason {
        HISTORY_INCOMPLETE, NO_CONFIRMED_SELLER_CLOSES, TARGET_NOT_PROVIDED, TARGET_PERIOD_MISMATCH,
        TARGET_MEASURE_MISMATCH, ZERO_TARGET, NEGATIVE_OBSERVATIONS_REQUIRE_POLICY,
        SHIFT_CLASSIFICATION_NOT_PROVIDED, MULTIPLIER_NOT_PROVIDED, MULTIPLIER_PERIOD_MISMATCH,
        MULTIPLIER_MEASURE_MISMATCH, INCOMPLETE_CALCULATIONS, ZERO_BASELINE
    }
    public enum ShiftPeriod { DAY, NIGHT }
    public enum DayType { REGULAR, HOLIDAY }

    public record Window(LocalDate from, LocalDate to) {
        public Window { ShiftSaleHistoryRange.validateDates(from, to); }
    }

    public enum Measure {
        FUEL_GALLONS("GALLONS"), BASKET_COP("COP");
        private final String unit;
        Measure(String unit) { this.unit = unit; }
        public String unit() { return unit; }

        public BigDecimal value(ShiftSaleHistory sale) {
            var amounts = sale.sales();
            return switch (this) {
                case FUEL_GALLONS -> amounts.motorGallons().add(amounts.dieselGallons())
                        .add(amounts.extraGallons()).add(amounts.maxproGallons());
                case BASKET_COP -> amounts.basketDominusAmountCop();
            };
        }

        public boolean containsAdjustment(ShiftSaleHistory sale) {
            var amounts = sale.sales();
            return switch (this) {
                case FUEL_GALLONS -> List.of(amounts.motorGallons(), amounts.dieselGallons(),
                        amounts.extraGallons(), amounts.maxproGallons()).stream().anyMatch(v -> v.signum() < 0);
                case BASKET_COP -> amounts.basketDominusAmountCop().signum() < 0;
            };
        }
    }

    /** An already assigned target for exactly this worker, metric and inclusive period; never prorated here. */
    public record IndividualTarget(UUID workerId, Window window, Measure measure, BigDecimal amount, String source) {
        public IndividualTarget {
            validWorker(workerId); Objects.requireNonNull(window); Objects.requireNonNull(measure);
            Objects.requireNonNull(amount); requireSource(source);
            if (amount.signum() < 0) throw new IllegalArgumentException("Target cannot be negative");
        }
    }

    /** Historical classification supplied with provenance, keyed by the original close identity. */
    public record ShiftClassification(ShiftSaleIdentity identity, ShiftPeriod period, DayType dayType, String source) {
        public ShiftClassification {
            Objects.requireNonNull(identity); Objects.requireNonNull(period); Objects.requireNonNull(dayType); requireSource(source);
        }
    }

    /**
     * Explicit dimensionless MULTIPLIER, not an unspecified factor. Its source must define multiplication,
     * the requested period and measure. No multiplier is derived from 70/30 or from a shift's name.
     */
    public record ShiftMultiplier(Window window, Measure measure, ShiftPeriod period,
                                  BigDecimal multiplier, String source) {
        public ShiftMultiplier {
            Objects.requireNonNull(window); Objects.requireNonNull(measure); Objects.requireNonNull(period);
            Objects.requireNonNull(multiplier); requireSource(source);
            if (multiplier.signum() <= 0) throw new IllegalArgumentException("Multiplier must be positive");
        }
    }

    /** Exact numerator/denominator retained; only display values are rounded. Ranking uses cross products. */
    public record ExactRatio(BigDecimal numerator, BigDecimal denominator) implements Comparable<ExactRatio> {
        public ExactRatio {
            Objects.requireNonNull(numerator); Objects.requireNonNull(denominator);
            if (denominator.signum() <= 0) throw new IllegalArgumentException("Positive denominator required");
        }
        public BigDecimal percent(int scale) {
            validateScale(scale);
            return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, scale, RoundingMode.HALF_EVEN);
        }
        @Override public int compareTo(ExactRatio other) {
            return numerator.multiply(other.denominator).compareTo(other.numerator.multiply(denominator));
        }
    }

    /** All three use cases share one confirmed history snapshot and the same exact window/metric. */
    public record Report(String source, String unit, int outputScale, String roundingMode,
                         ComplianceReport compliance, ComparisonReport comparison,
                         NormalizationReport normalization) { }

    public record Coverage(LocalDate date, ShiftSaleHistoryRange.DayStatus status) { }
    public record Compliance(UUID workerId, String recordedName, BigDecimal sales, BigDecimal target,
                             Availability availability, Reason reason, ExactRatio ratio,
                             BigDecimal percent, String targetSource) { }
    public record ComplianceReport(Window window, Measure measure, Availability availability, Reason reason,
                                   List<Coverage> coverage, List<Compliance> sellers) {
        public ComplianceReport { coverage = List.copyOf(coverage); sellers = List.copyOf(sellers); }
    }

    /** Equal weekday, day type, period, exact worked code and effective role: different durations/roles never get pooled. */
    public record Scenario(DayOfWeek weekday, ShiftPeriod period, DayType dayType, String workedCode,
                           ShiftSaleHistory.EffectiveRole effectiveRole) { }
    public record Comparison(UUID workerId, String recordedName, Scenario scenario, BigDecimal sales,
                             int observations, BigDecimal sellerAverage, BigDecimal stationSales,
                             int stationObservations, BigDecimal stationAverage,
                             ExactRatio relativeAverage, BigDecimal relativePercent, Availability availability, Reason reason) { }
    public record ComparisonReport(Window window, Measure measure, Availability availability, Reason reason,
                                   String baselineDefinition, List<Coverage> coverage, List<Comparison> sellers) {
        public ComparisonReport { coverage = List.copyOf(coverage); sellers = List.copyOf(sellers); }
    }

    public record NormalizedSale(UUID workerId, String recordedName, Scenario scenario,
                                 BigDecimal sales, int observations, BigDecimal multiplier,
                                 BigDecimal normalizedSales, Availability availability,
                                 Reason reason, String multiplierSource) { }
    public record NormalizationReport(Window window, Measure measure, Availability availability, Reason reason,
                                      List<Coverage> coverage, List<NormalizedSale> sellers) {
        public NormalizationReport { coverage = List.copyOf(coverage); sellers = List.copyOf(sellers); }
    }

    public static void validateScale(int scale) {
        if (scale < 0 || scale > 12) throw new IllegalArgumentException("Display scale must be 0..12");
    }

    private static void validWorker(UUID workerId) {
        Objects.requireNonNull(workerId);
        if (workerId.equals(new UUID(0, 0))) throw new IllegalArgumentException("Worker UUID cannot be zero");
    }

    private static void requireSource(String source) {
        if (source == null || source.isBlank()) throw new IllegalArgumentException("Parameter provenance required");
    }
}
