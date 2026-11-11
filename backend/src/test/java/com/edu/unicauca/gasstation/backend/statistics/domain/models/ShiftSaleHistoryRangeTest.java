package com.edu.unicauca.gasstation.backend.statistics.domain.models;

import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.edu.unicauca.gasstation.backend.statistics.SellerPerformanceTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShiftSaleHistoryRangeTest {
    @Test
    void distinguishesEveryDateStatusAndMakesItsCollectionsImmutable() {
        var sale = sale(A, "1.000", "2.00");
        var source = new ArrayList<>(List.of(sale));
        var range = new ShiftSaleHistoryRange(DATE, DATE.plusDays(3), List.of(
                new ShiftSaleHistoryRange.Day(DATE, processed(DATE, 1), source),
                new ShiftSaleHistoryRange.Day(DATE.plusDays(1), processed(DATE.plusDays(1), 0), List.of()),
                new ShiftSaleHistoryRange.Day(DATE.plusDays(2), pending(DATE.plusDays(2)), List.of()),
                new ShiftSaleHistoryRange.Day(DATE.plusDays(3), null, List.of())));
        source.clear();

        assertThat(range.days()).extracting(ShiftSaleHistoryRange.Day::status).containsExactly(
                ShiftSaleHistoryRange.DayStatus.PROCESSED, ShiftSaleHistoryRange.DayStatus.PROCESSED_EMPTY,
                ShiftSaleHistoryRange.DayStatus.PENDING, ShiftSaleHistoryRange.DayStatus.ABSENT);
        assertThat(range.sales()).containsExactly(sale);
        assertThat(range.complete()).isFalse();
        assertThatThrownBy(() -> range.days().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsMissingUnorderedOrWrongDateCoverage() {
        assertThatThrownBy(() -> new ShiftSaleHistoryRange(DATE, DATE, List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ShiftSaleHistoryRange(DATE, DATE,
                List.of(new ShiftSaleHistoryRange.Day(DATE.plusDays(1), null, List.of()))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ShiftSaleHistoryRange.Day(DATE, processed(DATE.plusDays(1), 0), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsSalesWithoutAConfirmedMatchingJournalAndCount() {
        var sale = sale(A, "1", "2");
        assertThatThrownBy(() -> new ShiftSaleHistoryRange.Day(DATE, null, List.of(sale)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ShiftSaleHistoryRange.Day(DATE, pending(DATE), List.of(sale)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ShiftSaleHistoryRange.Day(DATE, processed(DATE, 2), List.of(sale)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ShiftSaleHistoryRange.Day(DATE, processed(DATE, 2), List.of(sale, sale)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validatesRangesBeforeAnyQueryAndAcceptsYearBoundaries() {
        assertThatThrownBy(() -> new Window(DATE, DATE.minusDays(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Window(DATE, DATE.plusDays(3660))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Window(null, DATE)).isInstanceOf(NullPointerException.class);
        assertThat(new Window(DATE.withMonth(12).withDayOfMonth(31), DATE.plusYears(1).withMonth(1).withDayOfMonth(1)))
                .isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void zeroOrNegativeMultipliersAndRatioDenominatorsAreRejected(String value) {
        assertThatThrownBy(() -> new ShiftMultiplier(SINGLE_DAY, Measure.FUEL_GALLONS, ShiftPeriod.NIGHT,
                new BigDecimal(value), "explicit")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExactRatio(BigDecimal.ONE, new BigDecimal(value)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parametersRequireProvenanceAValidWorkerAndNonnegativeTargets() {
        assertThatThrownBy(() -> new IndividualTarget(A, SINGLE_DAY, Measure.FUEL_GALLONS, BigDecimal.ONE, " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new IndividualTarget(new UUID(0, 0), SINGLE_DAY,
                Measure.FUEL_GALLONS, BigDecimal.ONE, "explicit")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new IndividualTarget(A, SINGLE_DAY, Measure.FUEL_GALLONS,
                new BigDecimal("-1"), "explicit")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new IndividualTarget(A, SINGLE_DAY, Measure.FUEL_GALLONS, null, "explicit"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ShiftClassification(sale(A, "1", "2").identity(), ShiftPeriod.DAY, DayType.REGULAR, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
