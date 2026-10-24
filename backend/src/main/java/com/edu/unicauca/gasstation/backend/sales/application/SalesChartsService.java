package com.edu.unicauca.gasstation.backend.sales.application;

import com.edu.unicauca.gasstation.backend.sales.api.dto.StatisticsChartResponse;
import com.edu.unicauca.gasstation.backend.sales.api.dto.StatisticsChartResponse.*;
import com.edu.unicauca.gasstation.backend.sales.domain.calculation.*;
import com.edu.unicauca.gasstation.backend.sales.domain.calculation.SalesJourneyStatistics.*;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesTargetCategory;
import com.edu.unicauca.gasstation.backend.sales.domain.services.SalesTargetService;
import com.edu.unicauca.gasstation.backend.sales.exception.SalesTargetNotFoundException;
import java.math.*;
import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SalesChartsService {
    private final SalesTargetService targets;
    private final SalesDistributionService distribution;

    public StatisticsChartResponse observations(LocalDate from, LocalDate to, SalesTargetCategory category,
            List<JourneySale> observations, StatisticsPeriod period, int scale) {
        StatisticsPeriod.validate(from, to, scale);
        // Reuse SCRUM-40 date/journey uniqueness, units and exact-average semantics.
        var averages = new SalesJourneyStatistics().averages(from, to, category, observations);
        var series = new ArrayList<Series>();
        for (var journey : Journey.values()) {
            var values = observations.stream().filter(s -> s.journey() == journey && !s.date().isBefore(from) && !s.date().isAfter(to))
                    .map(s -> new StatisticsSeries.Value(s.date(), s.value())).toList();
            var buckets = StatisticsSeries.aggregate(values, period, scale);
            if (buckets.isEmpty()) continue;
            String label = journey == Journey.DAY ? "Diurna" : "Nocturna";
            series.add(new Series(journey + "/total", label, ChartType.LINE, Metric.OBSERVED_TOTAL, Unit.valueOf(averages.unit().name()), null, null,
                    buckets.stream().map(b -> new Point(b.start().toString(), b.start(), b.total(), b.observations(), b.change(), b.changePercent())).toList()));
            series.add(new Series(journey + "/average", label + " / Promedio por observación", ChartType.LINE, Metric.AVERAGE_PER_JOURNEY_OBSERVATION,
                    Unit.valueOf(averages.unit().name()), null, null,
                    buckets.stream().map(b -> new Point(b.start().toString(), b.start(), b.average(), b.observations(), null, null)).toList()));
        }
        return StatisticsChartResponse.of(Source.CLIENT_SUPPLIED_OBSERVATIONS, series.isEmpty() ? Availability.NO_DATA : Availability.AVAILABLE,
                series.isEmpty() ? "NO_OBSERVATIONS" : null, from, to, scale, series);
    }

    public StatisticsChartResponse target(long targetId, int scale) {
        validIdAndScale(targetId, scale);
        var target = targets.findById(targetId).orElseThrow(SalesTargetNotFoundException::new).target();
        var period = target.identity().period();
        var key = period.isAnnual() ? Integer.toString(period.year()) : "%d-%02d".formatted(period.year(), period.month());
        var series = new ArrayList<Series>();
        series.add(new Series("target", "Meta configurada", ChartType.BAR, Metric.CONFIGURED_TARGET, Unit.valueOf(target.unit().name()), null, null,
                List.of(new Point(key, null, target.value(), 0, null, null))));
        if (target.maximumSale() != null)
            series.add(new Series("maximumSale", "Venta máxima configurada", ChartType.BAR, Metric.CONFIGURED_MAXIMUM_SALE, Unit.valueOf(target.unit().name()), null, null,
                    List.of(new Point(key, null, target.maximumSale(), 0, null, null))));
        return StatisticsChartResponse.projection(null, null, scale, "UNROUNDED_STORED_VALUES",
                new ProjectionContext(targetId, null, null, null, null), series);
    }

    public StatisticsChartResponse distribution(long targetId, long profileId, int scale, List<DistributionCalculation.Assignment> assignments) {
        validIdAndScale(targetId, scale);
        if (profileId <= 0) throw new IllegalArgumentException("Positive profile ID required");
        var result = distribution.calculate(targetId, profileId, scale, assignments);
        var series = new ArrayList<Series>();
        for (boolean official : new boolean[]{true, false}) {
            var metric = official ? Metric.OFFICIAL_TARGET : Metric.REFERENCE_TARGET;
            String label = official ? "Meta oficial" : "Meta de referencia";
            series.add(new Series(metric + "/date", label, ChartType.LINE, metric, Unit.valueOf(result.target().unit().name()), null, null,
                    result.byDate().entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e -> new Point(e.getKey().toString(), e.getKey(),
                            official ? e.getValue().official() : e.getValue().reference(), 0, null, null)).toList()));
            series.add(new Series(metric + "/worker", label, ChartType.BAR, metric, Unit.valueOf(result.target().unit().name()), null, null,
                    result.byWorker().entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e -> new Point(e.getKey().toString(), null,
                            official ? e.getValue().official() : e.getValue().reference(), 0, null, null)).toList()));
        }
        return StatisticsChartResponse.projection(
                result.byDate().keySet().stream().min(LocalDate::compareTo).orElse(null),
                result.byDate().keySet().stream().max(LocalDate::compareTo).orElse(null), scale, result.decimalPolicy(),
                new ProjectionContext(targetId, profileId, result.parameterSource(), result.assignmentSource(), result.uncoveredHours().size()), series);
    }

    private void validIdAndScale(long id, int scale) {
        if (id <= 0) throw new IllegalArgumentException("Positive target ID required");
        if (scale < 0 || scale > 12) throw new IllegalArgumentException("outputScale must be 0..12");
    }
}
