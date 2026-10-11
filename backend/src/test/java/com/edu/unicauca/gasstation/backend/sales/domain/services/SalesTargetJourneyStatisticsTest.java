package com.edu.unicauca.gasstation.backend.sales.domain.services;

import com.edu.unicauca.gasstation.backend.sales.domain.calculation.ExactFraction;
import com.edu.unicauca.gasstation.backend.sales.domain.calculation.SalesJourneyStatistics;
import com.edu.unicauca.gasstation.backend.sales.domain.calculation.SalesJourneyStatistics.*;
import com.edu.unicauca.gasstation.backend.sales.domain.models.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

/** All amounts are explicit synthetic fixtures, never a connected historical sales source. */
class SalesTargetJourneyStatisticsTest {
    private final SalesJourneyStatistics statistics = new SalesJourneyStatistics();
    private final LocalDate monday = LocalDate.of(2026, 8, 3);
    private JourneySale sale(LocalDate date, Journey journey, String value) {
        return new JourneySale(date, journey, new BigDecimal(value));
    }

    @Test void hu16SeparatesJourneysAndFiltersInclusiveDates() {
        var rows = List.of(sale(monday.minusDays(1), Journey.DAY, "999"),
                sale(monday, Journey.DAY, "10"), sale(monday, Journey.NIGHT, "4"),
                sale(monday.plusDays(1), Journey.DAY, "20"), sale(monday.plusDays(1), Journey.NIGHT, "8"),
                sale(monday.plusDays(2), Journey.NIGHT, "999"));
        var result = statistics.averages(monday, monday.plusDays(1), SalesTargetCategory.FUEL_AGGREGATE, rows);
        assertThat(result.unit()).isEqualTo(SalesTargetUnit.GALLONS);
        assertThat(result.journeys().get(Journey.DAY).average()).contains(ExactFraction.of(new BigDecimal("15")));
        assertThat(result.journeys().get(Journey.NIGHT).average()).contains(ExactFraction.of(new BigDecimal("6")));
        assertThat(result.journeys().get(Journey.DAY).observations()).isEqualTo(2);
        assertThat(result.journeys().get(Journey.DAY).total()).isEqualByComparingTo("30");
    }

    @Test void hu17ComparesOnlyTheTwoRequestedWeekdayJourneyScenarios() {
        var rows = List.of(sale(monday, Journey.DAY, "10"), sale(monday.plusWeeks(1), Journey.DAY, "20"),
                sale(monday, Journey.NIGHT, "8"), sale(monday.plusDays(1), Journey.DAY, "999"));
        var day = new Scenario(DayOfWeek.MONDAY, Journey.DAY);
        var night = new Scenario(DayOfWeek.MONDAY, Journey.NIGHT);
        var result = statistics.compare(monday, monday.plusWeeks(1), SalesTargetCategory.BASKET, day, night, rows);
        assertThat(result.unit()).isEqualTo(SalesTargetUnit.COP);
        assertThat(result.first().scenario()).isEqualTo(day);
        assertThat(result.second().scenario()).isEqualTo(night);
        assertThat(result.first().sales().average()).contains(ExactFraction.of(new BigDecimal("15")));
        assertThat(result.second().sales().average()).contains(ExactFraction.of(new BigDecimal("8")));
        assertThat(result.second().sales().observations()).isEqualTo(1);
    }

    @Test void numericZerosCountAsObservationsUnlikeMissingJourneys() {
        var result = statistics.averages(monday, monday.plusDays(1), SalesTargetCategory.FUEL_AGGREGATE,
                List.of(sale(monday, Journey.DAY, "0"), sale(monday.plusDays(1), Journey.DAY, "10")));
        assertThat(result.journeys().get(Journey.DAY).average()).contains(ExactFraction.of(new BigDecimal("5")));
        assertThat(result.journeys().get(Journey.NIGHT).average()).isEmpty();
        assertThat(result.journeys().get(Journey.NIGHT).observations()).isZero();
    }

    @Test void repeatingAverageUsesExistingExactFractionWithoutRounding() {
        var result = statistics.averages(monday, monday.plusDays(2), SalesTargetCategory.FUEL_AGGREGATE,
                List.of(sale(monday, Journey.DAY, "1"), sale(monday.plusDays(1), Journey.DAY, "1"),
                        sale(monday.plusDays(2), Journey.DAY, "2")));
        assertThat(result.journeys().get(Journey.DAY).average())
                .contains(new ExactFraction(BigInteger.valueOf(4), BigInteger.valueOf(3)));
    }

    @Test void emptyAndSameScenarioComparisonsAreExplicitAndDeterministic() {
        var scenario = new Scenario(DayOfWeek.MONDAY, Journey.DAY);
        var empty = statistics.compare(monday, monday, SalesTargetCategory.BASKET, scenario, scenario, List.of());
        assertThat(empty.first().sales().average()).isEmpty();
        assertThat(empty.first()).isEqualTo(empty.second());
        var rows = new ArrayList<>(List.of(sale(monday, Journey.DAY, "1.2300000000001"),
                sale(monday.plusWeeks(1), Journey.DAY, "2E-13")));
        var first = statistics.compare(monday, monday.plusWeeks(1), SalesTargetCategory.BASKET, scenario, scenario, rows);
        Collections.reverse(rows);
        assertThat(statistics.compare(monday, monday.plusWeeks(1), SalesTargetCategory.BASKET, scenario, scenario, rows)).isEqualTo(first);
        assertThatThrownBy(() -> statistics.averages(monday, monday, SalesTargetCategory.BASKET, rows).journeys().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void invalidRangesAndDuplicateConsolidatedTotalsAreRejected() {
        assertThatThrownBy(() -> statistics.averages(monday, monday.minusDays(1), SalesTargetCategory.BASKET, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        var repeated = List.of(sale(monday, Journey.DAY, "1"), sale(monday, Journey.DAY, "2"));
        assertThatThrownBy(() -> statistics.averages(monday, monday, SalesTargetCategory.BASKET, repeated))
                .isInstanceOf(IllegalArgumentException.class);
        var scenario = new Scenario(DayOfWeek.MONDAY, Journey.DAY);
        assertThatThrownBy(() -> statistics.compare(monday, monday, SalesTargetCategory.BASKET, scenario, scenario, repeated))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void requiredInputMetadataCannotBeNull() {
        assertThatThrownBy(() -> new JourneySale(null, Journey.DAY, BigDecimal.ONE)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new JourneySale(monday, null, BigDecimal.ONE)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new JourneySale(monday, Journey.DAY, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Scenario(null, Journey.DAY)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Scenario(DayOfWeek.MONDAY, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> statistics.averages(null, monday, SalesTargetCategory.BASKET, List.of())).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> statistics.averages(monday, null, SalesTargetCategory.BASKET, List.of())).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> statistics.averages(monday, monday, null, List.of())).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> statistics.averages(monday, monday, SalesTargetCategory.BASKET, null)).isInstanceOf(NullPointerException.class);
    }

    @Test void highPrecisionAndSignedExplicitAmountsAreNotChangedOrAdjusted() {
        var value = new BigDecimal("1.234567890123456789000");
        var row = new JourneySale(monday, Journey.DAY, value);
        var result = statistics.averages(monday, monday, SalesTargetCategory.FUEL_AGGREGATE, List.of(row));
        assertThat(row.value()).isSameAs(value);
        assertThat(result.journeys().get(Journey.DAY).average()).contains(ExactFraction.of(value));
        // Mathematical input only: no refund policy or automatic adjustments are inferred.
        var signed = statistics.averages(monday, monday, SalesTargetCategory.FUEL_AGGREGATE,
                List.of(sale(monday, Journey.DAY, "-2")));
        assertThat(signed.journeys().get(Journey.DAY).average()).contains(ExactFraction.of(new BigDecimal("-2")));
    }
}
