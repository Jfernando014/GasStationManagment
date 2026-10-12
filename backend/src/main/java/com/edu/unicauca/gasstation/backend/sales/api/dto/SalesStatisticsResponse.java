package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.calculation.SalesJourneyStatistics.*;
import com.edu.unicauca.gasstation.backend.sales.domain.models.*;
import java.math.*;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;

/** Decimal display values have an explicit policy; exact fractions are also returned. */
public record SalesStatisticsResponse(LocalDate from, LocalDate to, SalesTargetCategory category,
        SalesTargetUnit unit, String source, int outputScale, String roundingMode, List<Series> series) {
    public record Fraction(String numerator, String denominator) { }
    public record Series(String label, DayOfWeek dayOfWeek, Journey journey, BigDecimal total,
                         int observations, boolean available, BigDecimal average, Fraction exactAverage) { }

    public static SalesStatisticsResponse from(JourneyAverages result, int scale) {
        var series = Arrays.stream(Journey.values()).map(j -> series(null, j, result.journeys().get(j), scale)).toList();
        return response(result.from(), result.to(), result.category(), result.unit(), scale, series);
    }
    public static SalesStatisticsResponse from(ScenarioComparison result, int scale) {
        return response(result.from(), result.to(), result.category(), result.unit(), scale,
                List.of(series(result.first().scenario().dayOfWeek(), result.first().scenario().journey(), result.first().sales(), scale),
                        series(result.second().scenario().dayOfWeek(), result.second().scenario().journey(), result.second().sales(), scale)));
    }
    private static SalesStatisticsResponse response(LocalDate from, LocalDate to, SalesTargetCategory category,
                                                     SalesTargetUnit unit, int scale, List<Series> series) {
        return new SalesStatisticsResponse(from, to, category, unit, "CLIENT_SUPPLIED_OBSERVATIONS", scale,
                RoundingMode.HALF_EVEN.name(), List.copyOf(series));
    }
    private static Series series(DayOfWeek day, Journey journey, Summary summary, int scale) {
        String label = journey == Journey.DAY ? "Diurna" : "Nocturna";
        if (day != null) label = day.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-CO")) + " / " + label;
        var exact = summary.average().orElse(null);
        return new Series(label, day, journey, summary.total(), summary.observations(), exact != null,
                exact == null ? null : new BigDecimal(exact.numerator()).divide(new BigDecimal(exact.denominator()), scale, RoundingMode.HALF_EVEN),
                exact == null ? null : new Fraction(exact.numerator().toString(), exact.denominator().toString()));
    }
}
