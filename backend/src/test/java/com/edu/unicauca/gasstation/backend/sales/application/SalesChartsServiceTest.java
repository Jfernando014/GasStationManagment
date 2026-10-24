package com.edu.unicauca.gasstation.backend.sales.application;

import com.edu.unicauca.gasstation.backend.sales.SalesTargetDistributionTestData;
import com.edu.unicauca.gasstation.backend.sales.api.dto.StatisticsChartResponse.*;
import com.edu.unicauca.gasstation.backend.sales.domain.calculation.*;
import com.edu.unicauca.gasstation.backend.sales.domain.calculation.SalesJourneyStatistics.*;
import com.edu.unicauca.gasstation.backend.sales.domain.models.*;
import com.edu.unicauca.gasstation.backend.sales.domain.services.SalesTargetService;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SalesChartsServiceTest {
    final SalesTargetService targets = mock(SalesTargetService.class);
    final SalesDistributionService distribution = mock(SalesDistributionService.class);
    final SalesChartsService service = new SalesChartsService(targets, distribution);
    final LocalDate from = LocalDate.of(2026, 8, 1), to = LocalDate.of(2026, 9, 30);
    JourneySale row(LocalDate date, String value) { return new JourneySale(date, Journey.DAY, new BigDecimal(value)); }

    @Test void observedPeriodsAreOrderedWithoutPaddedZerosAndTrendsCompareObservedTotals() {
        var chart = service.observations(from, to, SalesTargetCategory.BASKET,
                List.of(row(LocalDate.of(2026, 9, 3), "30"), row(from, "10"), row(from.plusDays(1), "10")), StatisticsPeriod.MONTH, 2);
        assertThat(chart.source()).isEqualTo(Source.CLIENT_SUPPLIED_OBSERVATIONS);
        assertThat(chart.series()).hasSize(2);
        var points = chart.series().getFirst().points();
        assertThat(points).extracting(Point::periodStart).containsExactly(from, LocalDate.of(2026, 9, 1));
        assertThat(points.getFirst().value()).isEqualByComparingTo("20");
        assertThat(points.getFirst().change()).isNull();
        assertThat(points.getLast().change()).isEqualByComparingTo("10");
        assertThat(points.getLast().changePercent()).isEqualByComparingTo("50.00");
        assertThat(chart.series().get(1).points().getFirst().value()).isEqualByComparingTo("10");
    }
    @Test void zeroBaselineHasAnAbsoluteChangeAndNoPercentage() {
        var chart = service.observations(from, to, SalesTargetCategory.FUEL_AGGREGATE,
                List.of(row(from, "0"), row(from.plusDays(3), "1")), StatisticsPeriod.DAY, 2);
        var points = chart.series().getFirst().points();
        assertThat(points).hasSize(2);
        assertThat(points.getLast().change()).isEqualByComparingTo("1");
        assertThat(points.getLast().changePercent()).isNull();
        assertThat(chart.series().getFirst().unit()).isEqualTo(Unit.GALLONS);
    }
    @Test void isoWeeksStartMondayAndMissingDatesAreNotInvented() {
        var values = List.of(new StatisticsSeries.Value(LocalDate.of(2026, 8, 2), BigDecimal.ONE),
                new StatisticsSeries.Value(LocalDate.of(2026, 8, 3), BigDecimal.TEN));
        assertThat(StatisticsSeries.aggregate(values, StatisticsPeriod.WEEK, 2)).extracting(StatisticsSeries.Bucket::start)
                .containsExactly(LocalDate.of(2026, 7, 27), LocalDate.of(2026, 8, 3));
    }
    @Test void averagesAgreeWithScrum40EngineAndRoundHalfEven() {
        var rows = List.of(row(from, "1.005"), row(from.plusDays(1), "1.005"));
        var existing = new SalesJourneyStatistics().averages(from, to, SalesTargetCategory.BASKET, rows);
        var chart = service.observations(from, to, SalesTargetCategory.BASKET, rows, StatisticsPeriod.MONTH, 2);
        var exact = existing.journeys().get(Journey.DAY).average().orElseThrow();
        assertThat(chart.series().get(1).points().getFirst().value()).isEqualByComparingTo(
                new BigDecimal(exact.numerator()).divide(new BigDecimal(exact.denominator()), 2, java.math.RoundingMode.HALF_EVEN));
        assertThat(chart.series().get(1).points().getFirst().value()).isEqualByComparingTo("1.00");
        assertThat(chart.series().getFirst().points().getFirst().value()).isEqualByComparingTo("2.010");
    }
    @Test void emptyInputIsNoDataAndDuplicateDateJourneyOrNegativeInputIsRejected() {
        var empty = service.observations(from, to, SalesTargetCategory.BASKET, List.of(), StatisticsPeriod.DAY, 2);
        assertThat(empty.availability()).isEqualTo(Availability.NO_DATA);
        assertThat(empty.series()).isEmpty();
        var row = row(from, "1");
        assertThatThrownBy(() -> service.observations(from, to, SalesTargetCategory.BASKET, List.of(row, row), StatisticsPeriod.DAY, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.observations(from, to, SalesTargetCategory.BASKET, List.of(row(from, "-1")), StatisticsPeriod.DAY, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void configuredTargetsRetainValuesAndCannotBeLabelledAsRealSales() {
        var target = new SalesTarget(new SalesTargetIdentity(SalesTargetPeriod.monthly(2026, 8), SalesTargetCategory.BASKET),
                new BigDecimal("10.005"), SalesTargetUnit.COP, BigDecimal.TEN);
        when(targets.findById(1L)).thenReturn(Optional.of(new StoredSalesTarget(1L, target, Instant.EPOCH)));
        var chart = service.target(1L, 2);
        assertThat(chart.source()).isEqualTo(Source.TARGET_PROJECTION);
        assertThat(chart.series().getFirst().metric()).isEqualTo(Metric.CONFIGURED_TARGET);
        assertThat(chart.series().getFirst().points().getFirst().value()).isEqualByComparingTo("10.005");
        assertThat(chart.series().getFirst().points().getFirst().key()).isEqualTo("2026-08");
        verifyNoInteractions(distribution);
    }
    @Test void distributionChartsReuseTheExistingEngineWithoutRecalculatingAllocations() {
        var target = new SalesTarget(new SalesTargetIdentity(SalesTargetPeriod.monthly(2026, 8), SalesTargetCategory.FUEL_AGGREGATE),
                new BigDecimal("26920"), SalesTargetUnit.GALLONS, new BigDecimal("30000"));
        var result = new SalesTargetDistributionEngine().calculate(new DistributionCalculation.Input(target,
                SalesTargetDistributionTestData.workbook(), SalesTargetDistributionTestData.fullMonth(), 2));
        when(distribution.calculate(1, 2, 2, List.of())).thenReturn(result);
        var chart = service.distribution(1, 2, 2, List.of());
        assertThat(chart.source()).isEqualTo(Source.TARGET_PROJECTION);
        assertThat(chart.series()).hasSize(4);
        assertThat(chart.decimalPolicy()).isEqualTo(result.decimalPolicy());
        assertThat(chart.roundingMode()).isNull();
        assertThat(chart.projection().assignmentSource()).isEqualTo(result.assignmentSource());
        assertThat(chart.series().getFirst().points()).hasSize(31);
        assertThat(chart.series().getFirst().points().stream().map(Point::value).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo(result.monthlyTotal().official());
        assertThat(chart.series().get(1).points()).hasSize(3);
        verify(distribution).calculate(1, 2, 2, List.of());
        verifyNoInteractions(targets);
    }
    @Test void invalidTargetIdsAndPrecisionDoNotReachRepositories() {
        assertThatThrownBy(() -> service.target(0, 2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.target(1, 13)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.distribution(1, -1, 2, List.of())).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(targets, distribution);
    }
}
