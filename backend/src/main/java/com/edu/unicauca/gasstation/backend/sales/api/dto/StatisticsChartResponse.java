package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Units are per series; an unavailable metric has no fabricated points. */
public record StatisticsChartResponse(Source source, Availability availability, String reason,
        LocalDate from, LocalDate to, int outputScale, String roundingMode, String decimalPolicy,
        ProjectionContext projection, List<Series> series) {
    public enum Source { CLIENT_SUPPLIED_OBSERVATIONS, TARGET_PROJECTION, REAL_HISTORICAL_SALES }
    public enum Availability { AVAILABLE, NO_DATA, UNAVAILABLE }
    public enum ChartType { BAR, LINE }
    public enum Metric {
        OBSERVED_TOTAL, AVERAGE_PER_JOURNEY_OBSERVATION, CONFIGURED_TARGET, CONFIGURED_MAXIMUM_SALE,
        OFFICIAL_TARGET, REFERENCE_TARGET, RECORDED_AMOUNT, RECORDED_QUANTITY, PARTICIPATION, AVERAGE_PER_WORKED_SHIFT
    }
    public enum Unit { COP, GALLONS, PRODUCT_UNITS, PERCENT, COP_PER_WORKED_SHIFT }
    public record ProjectionContext(long targetId, Long profileId, String parameterSource,
                                    String assignmentSource, Integer uncoveredHourCount) { }
    public record Point(String key, LocalDate periodStart, BigDecimal value, int observations,
                        BigDecimal change, BigDecimal changePercent) { }
    public record Series(String id, String label, ChartType chartType, Metric metric, Unit unit,
                         UUID workerId, ProductCategory category, List<Point> points) {
        public Series { points = List.copyOf(points); }
    }
    public StatisticsChartResponse { series = List.copyOf(series); }

    public static StatisticsChartResponse of(Source source, Availability availability, String reason,
            LocalDate from, LocalDate to, int scale, List<Series> series) {
        return new StatisticsChartResponse(source, availability, reason, from, to, scale,
                RoundingMode.HALF_EVEN.name(), "EXACT_TOTALS_HALF_EVEN_DERIVED_VALUES", null, series);
    }

    public static StatisticsChartResponse projection(LocalDate from, LocalDate to, int scale, String policy,
            ProjectionContext context, List<Series> series) {
        return new StatisticsChartResponse(Source.TARGET_PROJECTION, Availability.AVAILABLE,
                context.uncoveredHourCount() != null && context.uncoveredHourCount() > 0 ? "PARTIAL_SHIFT_COVERAGE" : null,
                from, to, scale, null, policy, context, series);
    }
}
