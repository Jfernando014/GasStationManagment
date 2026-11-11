package com.edu.unicauca.gasstation.backend.statistics.domain.calculation;

import com.edu.unicauca.gasstation.backend.statistics.domain.models.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.edu.unicauca.gasstation.backend.statistics.SellerPerformanceTestData.*;
import static com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.*;
import static com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.Availability.*;
import static com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.Measure.*;
import static com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.ShiftPeriod.*;
import static com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistory.EffectiveRole.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SellerPerformanceCalculatorTest {
    private final SellerPerformanceCalculator calculator = new SellerPerformanceCalculator();

    @Test
    void complianceUsesActualSalesAndSamePeriodIndividualTargetAndKeepsUnitsSeparate() {
        var history = history(SINGLE_DAY, sale(A, "12.345678901234567890", "250.12345678901234567890"));
        var fuel = calculator.compliance(history, FUEL_GALLONS,
                List.of(target(A, SINGLE_DAY, FUEL_GALLONS, "10.000")), 6);
        var basket = calculator.compliance(history, BASKET_COP,
                List.of(target(A, SINGLE_DAY, BASKET_COP, "100.00")), 6);

        assertThat(fuel.availability()).isEqualTo(AVAILABLE);
        assertThat(fuel.sellers().getFirst().sales()).isEqualByComparingTo("12.345678901234567890");
        assertThat(fuel.sellers().getFirst().percent()).isEqualByComparingTo("123.456789");
        assertThat(fuel.sellers().getFirst().ratio().numerator()).isEqualByComparingTo("12.345678901234567890");
        assertThat(fuel.measure().unit()).isEqualTo("GALLONS");
        assertThat(basket.sellers().getFirst().sales()).isEqualByComparingTo("250.12345678901234567890");
        assertThat(basket.measure().unit()).isEqualTo("COP");
        assertThat(basket.sellers().getFirst().targetSource()).isNotBlank();
    }

    @Test
    void fuelAggregatesOnlyTheFourRecordedGallonFields() {
        var row = sale(A, "0", "123");
        var exact = new ShiftSaleHistory(row.journal(), row.identity(), A, row.vendorName(), row.workedCode(), TITULAR,
                new ShiftSaleAmounts(new BigDecimal("1.00001"), new BigDecimal("2.00002"),
                        new BigDecimal("3.00003"), new BigDecimal("4.00004"),
                        new BigDecimal("123"), new BigDecimal("555"), new BigDecimal("777")));
        var result = calculator.compliance(history(SINGLE_DAY, exact), FUEL_GALLONS,
                List.of(target(A, SINGLE_DAY, FUEL_GALLONS, "10")), 3);

        assertThat(result.sellers().getFirst().sales()).isEqualByComparingTo("10.00010");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6, 30, 365})
    void anExplicitTargetForTheWholeRequestedPeriodIsNeverProrated(int days) {
        var window = new Window(DATE, DATE.plusDays(days));
        var rows = days == 0 ? new ShiftSaleHistory[]{sale(A, "3", "0")}
                : new ShiftSaleHistory[]{sale(A, "1", "0"), sale(window.to(), A, "DIA", TITULAR, "2", "0")};
        var report = calculator.compliance(history(window, rows), FUEL_GALLONS,
                List.of(target(A, window, FUEL_GALLONS, "6")), 2);

        assertThat(report.sellers().getFirst().percent()).isEqualByComparingTo("50");
        assertThat(report.window()).isEqualTo(window);
    }

    @Test
    void rankingComparesExactRatiosBeforeRoundingAndHasDeterministicTies() {
        var report = calculator.compliance(history(SINGLE_DAY, sale(A, "1", "0"), sale(B, "1.0000000001", "0")),
                FUEL_GALLONS, List.of(target(A, SINGLE_DAY, FUEL_GALLONS, "3"),
                        target(B, SINGLE_DAY, FUEL_GALLONS, "3")), 2);

        assertThat(report.sellers()).extracting(Compliance::workerId).containsExactly(B, A);
        assertThat(report.sellers()).extracting(Compliance::percent).containsExactly(
                new BigDecimal("33.33"), new BigDecimal("33.33"));
    }

    @Test
    void exactRankingTiesUseUuidInsteadOfMutableNames() {
        var report = calculator.compliance(history(SINGLE_DAY, sale(B, "2", "0"), sale(A, "1", "0")),
                FUEL_GALLONS, List.of(target(A, SINGLE_DAY, FUEL_GALLONS, "3"),
                        target(B, SINGLE_DAY, FUEL_GALLONS, "6")), 2);
        assertThat(report.sellers()).extracting(Compliance::workerId).containsExactly(A, B);
    }

    @Test
    void absentTargetLeavesThatSellerUnavailableAndAfterTheCalculatedRanking() {
        var report = calculator.compliance(history(SINGLE_DAY, sale(A, "999", "0"), sale(B, "1", "0")),
                FUEL_GALLONS, List.of(target(B, SINGLE_DAY, FUEL_GALLONS, "2")), 2);

        assertThat(report.availability()).isEqualTo(PARTIAL);
        assertThat(report.sellers()).extracting(Compliance::workerId).containsExactly(B, A);
        assertThat(report.sellers().getLast().percent()).isNull();
        assertThat(report.sellers().getLast().reason()).isEqualTo(Reason.TARGET_NOT_PROVIDED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"period", "measure", "zero"})
    void incompatibleOrZeroTargetsNeverProduceAPercentage(String problem) {
        var target = switch (problem) {
            case "period" -> target(A, new Window(DATE, DATE.plusDays(30)), FUEL_GALLONS, "10");
            case "measure" -> target(A, SINGLE_DAY, BASKET_COP, "10");
            default -> target(A, SINGLE_DAY, FUEL_GALLONS, "0");
        };
        var result = calculator.compliance(history(SINGLE_DAY, sale(A, "1", "2")), FUEL_GALLONS, List.of(target), 2);

        assertThat(result.availability()).isEqualTo(UNAVAILABLE);
        assertThat(result.sellers().getFirst().percent()).isNull();
        assertThat(result.sellers().getFirst().reason()).isEqualTo(switch (problem) {
            case "period" -> Reason.TARGET_PERIOD_MISMATCH;
            case "measure" -> Reason.TARGET_MEASURE_MISMATCH;
            default -> Reason.ZERO_TARGET;
        });
    }

    @Test
    void aRecordedZeroIsAValidObservationButAConfirmedEmptyDayDoesNotInventSellerRows() {
        var observed = calculator.compliance(history(SINGLE_DAY, sale(A, "0.000", "0.00")), FUEL_GALLONS,
                List.of(target(A, SINGLE_DAY, FUEL_GALLONS, "10")), 2);
        var empty = calculator.compliance(history(SINGLE_DAY), FUEL_GALLONS,
                List.of(target(A, SINGLE_DAY, FUEL_GALLONS, "10")), 2);

        assertThat(observed.availability()).isEqualTo(AVAILABLE);
        assertThat(observed.sellers().getFirst().percent()).isZero();
        assertThat(empty.availability()).isEqualTo(NO_DATA);
        assertThat(empty.sellers()).isEmpty();
        assertThat(empty.coverage().getFirst().status()).isEqualTo(ShiftSaleHistoryRange.DayStatus.PROCESSED_EMPTY);
    }

    @ParameterizedTest
    @ValueSource(strings = {"pending", "absent"})
    void anIncompleteDatePreventsPartialPeriodCalculations(String status) {
        var row = sale(A, "10", "20");
        var range = new ShiftSaleHistoryRange(DATE, DATE.plusDays(1), List.of(
                new ShiftSaleHistoryRange.Day(DATE, processed(DATE, 1), List.of(row)),
                new ShiftSaleHistoryRange.Day(DATE.plusDays(1),
                        status.equals("pending") ? pending(DATE.plusDays(1)) : null, List.of())));
        var compliance = calculator.compliance(range, FUEL_GALLONS, List.of(), 2);
        var comparison = calculator.compare(range, FUEL_GALLONS, List.of(), 2);
        var normalized = calculator.normalize(range, FUEL_GALLONS, List.of(), List.of(), 2);

        assertThat(compliance.reason()).isEqualTo(Reason.HISTORY_INCOMPLETE);
        assertThat(comparison.reason()).isEqualTo(Reason.HISTORY_INCOMPLETE);
        assertThat(normalized.reason()).isEqualTo(Reason.HISTORY_INCOMPLETE);
        assertThat(compliance.sellers()).isEmpty();
        assertThat(comparison.sellers()).isEmpty();
        assertThat(normalized.sellers()).isEmpty();
    }

    @Test
    void adjustmentsRemainUnavailableWithoutChangingTheRecordedSnapshotOrAnotherMeasure() {
        var row = sale(A, "-0.001", "20.000");
        var history = history(SINGLE_DAY, row);
        var fuel = calculator.compliance(history, FUEL_GALLONS, List.of(), 2);
        var basket = calculator.compliance(history, BASKET_COP,
                List.of(target(A, SINGLE_DAY, BASKET_COP, "10")), 2);

        assertThat(fuel.reason()).isEqualTo(Reason.NEGATIVE_OBSERVATIONS_REQUIRE_POLICY);
        assertThat(basket.sellers().getFirst().percent()).isEqualByComparingTo("200");
        assertThat(history.sales().getFirst().sales().motorGallons()).isEqualTo(new BigDecimal("-0.001"));
    }

    @Test
    void comparisonsKeepWeekdayPeriodCodeAndEffectiveRoleSeparate() {
        var history = history(new Window(DATE, DATE.plusDays(1)),
                sale(A, "100", "0"), sale(B, "300", "0"),
                sale(DATE, A, "NOCHE", TITULAR, "50", "0"),
                sale(DATE, B, "DIA6", TITULAR, "900", "0"),
                sale(DATE, B, "12-7", APOYO, "40", "0"),
                sale(DATE.plusDays(1), A, "DIA", TITULAR, "1000", "0"));
        var classes = history.sales().stream().map(sale -> new ShiftClassification(sale.identity(),
                sale.workedCode().equals("NOCHE") ? NIGHT : DAY, DayType.REGULAR, "explicit source")).toList();

        var result = calculator.compare(history, FUEL_GALLONS, classes, 2);
        var first = result.sellers().stream().filter(row -> row.workerId().equals(A)
                && row.scenario().weekday() == DATE.getDayOfWeek() && row.scenario().workedCode().equals("DIA"))
                .findFirst().orElseThrow();

        assertThat(result.availability()).isEqualTo(AVAILABLE);
        assertThat(result.sellers()).hasSize(6);
        assertThat(first.stationObservations()).isEqualTo(2);
        assertThat(first.stationAverage()).isEqualByComparingTo("200");
        assertThat(first.relativePercent()).isEqualByComparingTo("50");
        assertThat(result.baselineDefinition()).isEqualTo(SellerPerformanceCalculator.BASELINE);
    }

    @Test
    void effectiveRoleSeparatesEvenClosesWithTheSameWorkedCode() {
        var history = history(SINGLE_DAY, sale(A, "100", "0"), sale(DATE, B, "DIA", APOYO, "30", "0"));
        var result = calculator.compare(history, FUEL_GALLONS, classified(history, DAY), 2);
        assertThat(result.sellers()).allSatisfy(row -> assertThat(row.stationObservations()).isEqualTo(1));
    }

    @Test
    void stationMeanWeightsRecordedEquivalentShiftsInsteadOfAveragingSellerAverages() {
        var history = history(new Window(DATE, DATE.plusDays(7)),
                sale(A, "100", "0"), sale(DATE.plusDays(7), A, "DIA", TITULAR, "300", "0"),
                sale(B, "300", "0"));
        var result = calculator.compare(history, FUEL_GALLONS, classified(history, DAY), 2);
        var seller = result.sellers().stream().filter(row -> row.workerId().equals(A)).findFirst().orElseThrow();

        assertThat(seller.observations()).isEqualTo(2);
        assertThat(seller.stationObservations()).isEqualTo(3);
        assertThat(seller.sellerAverage()).isEqualByComparingTo("200");
        assertThat(seller.stationAverage()).isEqualByComparingTo("233.33");
        assertThat(seller.relativePercent()).isEqualByComparingTo("85.71");
        assertThat(seller.relativeAverage().numerator()).isEqualByComparingTo("1200");
        assertThat(seller.relativeAverage().denominator()).isEqualByComparingTo("1400");
    }

    @Test
    void allZeroStationObservationsHaveValidAveragesButNoRelativeIndex() {
        var history = history(SINGLE_DAY, sale(A, "0", "0"), sale(B, "0", "0"));
        var result = calculator.compare(history, FUEL_GALLONS, classified(history, DAY), 2);
        assertThat(result.sellers()).allSatisfy(row -> {
            assertThat(row.stationAverage()).isZero();
            assertThat(row.relativePercent()).isNull();
            assertThat(row.reason()).isEqualTo(Reason.ZERO_BASELINE);
        });
    }

    @Test
    void aMissingHistoricalClassificationDoesNotProduceABiasedPartialStationMean() {
        var history = history(SINGLE_DAY, sale(A, "100", "0"), sale(B, "300", "0"));
        var partial = List.of(new ShiftClassification(history.sales().getFirst().identity(), DAY, DayType.REGULAR, "explicit"));

        assertThat(calculator.compare(history, FUEL_GALLONS, partial, 2).reason())
                .isEqualTo(Reason.SHIFT_CLASSIFICATION_NOT_PROVIDED);
        assertThat(calculator.normalize(history, FUEL_GALLONS, partial, List.of(), 2).sellers()).isEmpty();
    }

    @Test
    void normalizationRequiresExplicitMultipliersAndDoesNotUseSupportShares() {
        var history = history(SINGLE_DAY, sale(A, "100.12345678901234567890", "10"),
                sale(DATE, B, "12-7", APOYO, "30", "0"));
        var classes = classified(history, DAY);
        var missing = calculator.normalize(history, FUEL_GALLONS, classes, List.of(), 2);
        assertThat(missing.sellers()).allSatisfy(row -> {
            assertThat(row.normalizedSales()).isNull();
            assertThat(row.reason()).isEqualTo(Reason.MULTIPLIER_NOT_PROVIDED);
        });

        var factor = new ShiftMultiplier(SINGLE_DAY, FUEL_GALLONS, DAY,
                new BigDecimal("1.25000000001"), "explicit multiplication rule fixture");
        var result = calculator.normalize(history, FUEL_GALLONS, classes, List.of(factor), 2);
        assertThat(result.sellers().getFirst().normalizedSales()).isEqualTo(
                new BigDecimal("100.12345678901234567890").multiply(factor.multiplier()));
        assertThat(result.sellers().getLast().normalizedSales()).isEqualByComparingTo(
                new BigDecimal("30").multiply(factor.multiplier()));
        assertThat(result.sellers().getLast().scenario().effectiveRole()).isEqualTo(APOYO);
    }

    @Test
    void configuredDayAndNightMultipliersRemainSeparateAndMissingOneIsExplicit() {
        var history = history(SINGLE_DAY, sale(A, "10", "10"), sale(DATE, A, "NOCHE", TITULAR, "5", "20"));
        var classes = history.sales().stream().map(s -> new ShiftClassification(s.identity(),
                s.workedCode().equals("DIA") ? DAY : NIGHT, DayType.REGULAR, "explicit")).toList();
        var result = calculator.normalize(history, BASKET_COP, classes,
                List.of(new ShiftMultiplier(SINGLE_DAY, BASKET_COP, DAY, new BigDecimal("2"), "explicit")), 2);

        assertThat(result.availability()).isEqualTo(PARTIAL);
        assertThat(result.sellers().getFirst().normalizedSales()).isEqualByComparingTo("20");
        assertThat(result.sellers().getLast().normalizedSales()).isNull();
        assertThat(result.sellers().getLast().reason()).isEqualTo(Reason.MULTIPLIER_NOT_PROVIDED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"period", "measure"})
    void aMultiplierCannotBeUsedForAnotherPeriodOrUnit(String problem) {
        var history = history(SINGLE_DAY, sale(A, "10", "20"));
        var multiplier = new ShiftMultiplier(problem.equals("period") ? new Window(DATE, DATE.plusDays(1)) : SINGLE_DAY,
                problem.equals("measure") ? BASKET_COP : FUEL_GALLONS, DAY, BigDecimal.TEN, "explicit");
        var result = calculator.normalize(history, FUEL_GALLONS, classified(history, DAY), List.of(multiplier), 2);

        assertThat(result.sellers().getFirst().normalizedSales()).isNull();
        assertThat(result.sellers().getFirst().reason()).isEqualTo(problem.equals("period")
                ? Reason.MULTIPLIER_PERIOD_MISMATCH : Reason.MULTIPLIER_MEASURE_MISMATCH);
    }

    @Test
    void ambiguousExplicitParametersAreRejectedInsteadOfChoosingOne() {
        var history = history(SINGLE_DAY, sale(A, "10", "20"));
        var target = target(A, SINGLE_DAY, FUEL_GALLONS, "10");
        var classification = classified(history, DAY).getFirst();
        var factor = new ShiftMultiplier(SINGLE_DAY, FUEL_GALLONS, DAY, BigDecimal.ONE, "explicit");
        assertThatThrownBy(() -> calculator.compliance(history, FUEL_GALLONS, List.of(target, target), 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.compare(history, FUEL_GALLONS, List.of(classification, classification), 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.normalize(history, FUEL_GALLONS, List.of(classification),
                List.of(factor, factor), 2)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ratiosPreserveExactFractionsAndUseHalfEvenOnlyForDisplay() {
        assertThat(new ExactRatio(new BigDecimal("1.005"), new BigDecimal("100")).percent(2))
                .isEqualTo(new BigDecimal("1.00"));
        assertThat(new ExactRatio(BigDecimal.ONE, new BigDecimal("3")).percent(2)).isEqualTo(new BigDecimal("33.33"));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 13})
    void invalidDisplayPrecisionIsRejected(int scale) {
        assertThatThrownBy(() -> calculator.compliance(history(SINGLE_DAY), FUEL_GALLONS, List.of(), scale))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void explicitHolidayAndOrdinaryWeekdaysNeverShareAStationBaseline() {
        var nextMonday = DATE.plusDays(7);
        var history = history(new Window(DATE, nextMonday), sale(A, "100", "0"), sale(B, "300", "0"),
                sale(nextMonday, A, "DIA", TITULAR, "1000", "0"),
                sale(nextMonday, B, "DIA", TITULAR, "3000", "0"));
        var classes = history.sales().stream().map(s -> new ShiftClassification(s.identity(), DAY,
                s.journal().journalDate().equals(DATE) ? DayType.REGULAR : DayType.HOLIDAY,
                "explicit day-type fixture; no calendar lookup")).toList();

        var result = calculator.compare(history, FUEL_GALLONS, classes, 2);
        var ordinary = result.sellers().stream().filter(s -> s.workerId().equals(A)
                && s.scenario().dayType() == DayType.REGULAR).findFirst().orElseThrow();
        var holiday = result.sellers().stream().filter(s -> s.workerId().equals(A)
                && s.scenario().dayType() == DayType.HOLIDAY).findFirst().orElseThrow();

        assertThat(ordinary.stationObservations()).isEqualTo(2);
        assertThat(ordinary.stationAverage()).isEqualByComparingTo("200");
        assertThat(holiday.stationObservations()).isEqualTo(2);
        assertThat(holiday.stationAverage()).isEqualByComparingTo("2000");
    }

    @Test
    void contradictoryExplicitDayTypesForOneDateAreRejected() {
        var history = history(SINGLE_DAY, sale(A, "100", "0"), sale(B, "300", "0"));
        var classes = List.of(new ShiftClassification(history.sales().getFirst().identity(), DAY, DayType.REGULAR, "explicit"),
                new ShiftClassification(history.sales().getLast().identity(), DAY, DayType.HOLIDAY, "explicit"));

        assertThatThrownBy(() -> calculator.compare(history, FUEL_GALLONS, classes, 2))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Conflicting");
    }


    @Test
    void reportCollectionsCannotBeMutated() {
        var result = calculator.compliance(history(SINGLE_DAY, sale(A, "1", "2")), FUEL_GALLONS, List.of(), 2);
        assertThatThrownBy(() -> result.sellers().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.coverage().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
