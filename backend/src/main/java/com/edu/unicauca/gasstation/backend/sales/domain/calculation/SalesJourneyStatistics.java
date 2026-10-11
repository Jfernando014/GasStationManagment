package com.edu.unicauca.gasstation.backend.sales.domain.calculation;

import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesTargetCategory;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesTargetUnit;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

/**
 * HU-16/HU-17 mathematics over explicitly supplied, homogeneous journey totals.
 * No live-sales source, hour classification, adjustments or holiday rules are inferred.
 * ESCENARIOS B3:B16 averages numeric observations, including zero, unlike E2:E8.
 */
public final class SalesJourneyStatistics {
    public enum Journey { DAY, NIGHT }

    /** One already consolidated sale amount per calendar date/journey, in the requested category. */
    public record JourneySale(LocalDate date, Journey journey, BigDecimal value) {
        public JourneySale {
            Objects.requireNonNull(date, "date");
            Objects.requireNonNull(journey, "journey");
            Objects.requireNonNull(value, "value");
        }
    }

    public record Scenario(DayOfWeek dayOfWeek, Journey journey) {
        public Scenario {
            Objects.requireNonNull(dayOfWeek, "dayOfWeek");
            Objects.requireNonNull(journey, "journey");
        }
    }

    /** An empty average means no observations; a recorded zero is an observation. */
    public record Summary(BigDecimal total, int observations, Optional<ExactFraction> average) { }
    public record JourneyAverages(LocalDate from, LocalDate to, SalesTargetCategory category,
                                  SalesTargetUnit unit, Map<Journey, Summary> journeys) {
        public JourneyAverages { journeys = Map.copyOf(journeys); }
    }
    public record ScenarioResult(Scenario scenario, Summary sales) { }
    public record ScenarioComparison(LocalDate from, LocalDate to, SalesTargetCategory category,
                                     SalesTargetUnit unit, ScenarioResult first, ScenarioResult second) { }

    public JourneyAverages averages(LocalDate from, LocalDate to, SalesTargetCategory category,
                                    List<JourneySale> explicitSales) {
        var selected = select(from, to, category, explicitSales);
        var summaries = new EnumMap<Journey, Summary>(Journey.class);
        for (var journey : Journey.values())
            summaries.put(journey, summarize(selected.stream().filter(s -> s.journey() == journey).toList()));
        return new JourneyAverages(from, to, category, unit(category), summaries);
    }

    public ScenarioComparison compare(LocalDate from, LocalDate to, SalesTargetCategory category,
                                       Scenario first, Scenario second, List<JourneySale> explicitSales) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        var selected = select(from, to, category, explicitSales);
        return new ScenarioComparison(from, to, category, unit(category),
                scenarioResult(first, selected), scenarioResult(second, selected));
    }

    private ScenarioResult scenarioResult(Scenario scenario, List<JourneySale> sales) {
        return new ScenarioResult(scenario, summarize(sales.stream()
                .filter(s -> s.journey() == scenario.journey() && s.date().getDayOfWeek() == scenario.dayOfWeek())
                .toList()));
    }

    private Summary summarize(List<JourneySale> sales) {
        var total = sales.stream().map(JourneySale::value).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Summary(total, sales.size(), sales.isEmpty() ? Optional.empty()
                : Optional.of(ExactFraction.of(total).divide(ExactFraction.of(BigDecimal.valueOf(sales.size())))));
    }

    private List<JourneySale> select(LocalDate from, LocalDate to, SalesTargetCategory category,
                                     List<JourneySale> sales) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(sales, "explicitSales");
        if (to.isBefore(from)) throw new IllegalArgumentException("Invalid sales date range");
        var selected = new ArrayList<JourneySale>();
        var seen = new HashMap<LocalDate, Set<Journey>>();
        for (var sale : sales) {
            Objects.requireNonNull(sale, "sale");
            if (sale.date().isBefore(from) || sale.date().isAfter(to)) continue;
            if (!seen.computeIfAbsent(sale.date(), ignored -> EnumSet.noneOf(Journey.class)).add(sale.journey()))
                throw new IllegalArgumentException("Duplicate date/journey total: " + sale.date() + "/" + sale.journey());
            selected.add(sale);
        }
        return List.copyOf(selected);
    }

    private SalesTargetUnit unit(SalesTargetCategory category) {
        return switch (category) {
            case FUEL_AGGREGATE -> SalesTargetUnit.GALLONS;
            case BASKET -> SalesTargetUnit.COP;
        };
    }
}
