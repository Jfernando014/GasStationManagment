package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

import static com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.ReconciliationTestData.august1to4;
import static com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.ReconciliationTestData.money;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * Acceptance figures from the Credibanco and Puntos Colombia August 2026 reports. Day 28 is synthetic: it carries
 * the remainder of the month because the report's complete daily breakdown was not provided.
 */
class MonthlyReconciliationCalculatorTest {

    private static final YearMonth AUGUST = YearMonth.of(2026, 8);
    private static final BigDecimal RATE = new BigDecimal("0.026500");

    private static DayFranchiseTotals day(String mastercard, String visa, String qr) {
        return new DayFranchiseTotals(money("0"), money(mastercard), money(visa), money("0"), money("0"), money("0"),
                money(qr));
    }

    private static Map<LocalDate, DayFranchiseTotals> augustCards() {
        Map<LocalDate, DayFranchiseTotals> cards = new TreeMap<>(august1to4());
        cards.put(LocalDate.of(2026, 8, 5), day("3618384", "2072928", "876008"));
        cards.put(LocalDate.of(2026, 8, 28), day("52735721", "24635985", "15886175"));
        return cards;
    }

    private static Map<LocalDate, BigDecimal> augustPoints() {
        Map<LocalDate, BigDecimal> points = new TreeMap<>();
        points.put(LocalDate.of(2026, 8, 1), money("46050"));
        points.put(LocalDate.of(2026, 8, 5), money("41380"));
        points.put(LocalDate.of(2026, 8, 28), money("1709525"));
        return points;
    }

    private static MonthlyReconciliation august() {
        return MonthlyReconciliationCalculator.calculate(AUGUST, augustCards(), Map.of(), augustPoints(), RATE);
    }

    private static MonthlyReconciliation of(YearMonth month, Map<LocalDate, DayFranchiseTotals> cards,
                                            Map<LocalDate, BigDecimal> redeban, Map<LocalDate, BigDecimal> points) {
        return MonthlyReconciliationCalculator.calculate(month, cards, redeban, points, RATE);
    }

    @Test
    void augustTotalsMatchTheClientReport() {
        MonthlyReconciliation result = august();

        assertEquals("2026-08", result.month());
        assertEquals(money("66683965"), result.totals().mastercard());
        assertEquals(money("32184828"), result.totals().visa());
        assertEquals(money("0"), result.totals().amex());
        assertEquals(money("98868793"), result.totals().totalEds());
        assertEquals(money("19476459"), result.totals().qr());
        assertEquals(money("118345252"), result.totals().totalWithQr());
        assertEquals(money("1796955"), result.totals().points());
    }

    @Test
    void augustCommissionIsTwoPointSixFivePercentOfCardsOnly() {
        MonthlyCommission commission = august().commission();

        assertEquals(RATE, commission.rate());
        assertEquals(LocalDate.of(2026, 8, 31), commission.rateDate());
        assertEquals(money("98868793"), commission.base());
        assertEquals(money("2620023.01"), commission.amount());
    }

    @Test
    void day5MatchesTheWorkbook() {
        MonthlyDayRow row = august().days().get(4);

        assertEquals(LocalDate.of(2026, 8, 5), row.date());
        assertEquals(money("3618384"), row.mastercard());
        assertEquals(money("2072928"), row.visa());
        assertEquals(money("5691312"), row.totalEds());
        assertEquals(money("876008"), row.qr());
        assertEquals(MonthlyDayRow.QrSource.PLANILLA, row.qrSource());
        assertEquals(money("6567320"), row.totalWithQr());
        assertEquals(money("41380"), row.points());
    }

    @Test
    void day5CommissionAloneIsTheExpectedHu34Figure() {
        Map<LocalDate, DayFranchiseTotals> only5 = Map.of(
                LocalDate.of(2026, 8, 5), day("3618384", "2072928", "876008"));

        MonthlyReconciliation result = of(AUGUST, only5, Map.of(), Map.of());

        assertEquals(money("5691312"), result.totals().totalEds());
        assertEquals(money("150819.77"), result.commission().amount());
    }

    @Test
    void theTableHasOneRowPerCalendarDayAndDaysWithoutMovementAreZero() {
        MonthlyReconciliation result = august();

        assertEquals(31, result.days().size());
        assertEquals(LocalDate.of(2026, 8, 1), result.from());
        assertEquals(LocalDate.of(2026, 8, 31), result.to());
        assertEquals(LocalDate.of(2026, 8, 1), result.days().get(0).date());
        assertEquals(LocalDate.of(2026, 8, 31), result.days().get(30).date());
        for (int index : new int[] {28, 29, 30}) {
            MonthlyDayRow row = result.days().get(index);
            assertFalse(row.hasData());
            assertEquals(money("0"), row.totalEds());
            assertEquals(money("0"), row.qr());
            assertEquals(MonthlyDayRow.QrSource.NONE, row.qrSource());
            assertEquals(money("0"), row.points());
        }
        assertEquals(6, result.daysWithData());
    }

    @Test
    void anEmptyMonthIsAllZerosWithNoCommission() {
        MonthlyReconciliation result = of(YearMonth.of(2026, 9), Map.of(), Map.of(), Map.of());

        assertEquals(30, result.days().size());
        assertEquals(0, result.daysWithData());
        assertEquals(money("0"), result.totals().totalEds());
        assertEquals(money("0"), result.totals().points());
        assertEquals(money("0"), result.commission().amount());
    }

    @Test
    void theNumberOfRowsFollowsTheMonthLength() {
        assertEquals(28, of(YearMonth.of(2026, 2), Map.of(), Map.of(), Map.of()).days().size());
        assertEquals(29, of(YearMonth.of(2028, 2), Map.of(), Map.of(), Map.of()).days().size());
    }

    @Test
    void redebanWinsOverThePlanillasWhenItsFileWasImported() {
        LocalDate day5 = LocalDate.of(2026, 8, 5);
        MonthlyReconciliation result = of(AUGUST, augustCards(), Map.of(day5, money("900000")), augustPoints());

        MonthlyDayRow row = result.days().get(4);
        assertEquals(MonthlyDayRow.QrSource.REDEBAN, row.qrSource());
        assertEquals(money("900000"), row.qr());
        assertEquals(money("6591312"), row.totalWithQr());
        assertEquals(money("19500451"), result.totals().qr());
        assertEquals(money("98868793"), result.commission().base());
    }

    @Test
    void aZeroRedebanDayFallsBackToThePlanillas() {
        LocalDate day6 = LocalDate.of(2026, 8, 6);
        Map<LocalDate, DayFranchiseTotals> cards = Map.of(day6, day("0", "0", "500"));

        MonthlyDayRow row = of(AUGUST, cards, Map.of(day6, money("0")), Map.of()).days().get(5);

        assertEquals(MonthlyDayRow.QrSource.PLANILLA, row.qrSource());
        assertEquals(money("500"), row.qr());
        assertTrue(row.hasData());
    }

    @Test
    void aDayWithOnlyAPointsTotalOrOnlyRedebanCountsAsData() {
        LocalDate day10 = LocalDate.of(2026, 8, 10);
        LocalDate day11 = LocalDate.of(2026, 8, 11);

        MonthlyReconciliation result = of(AUGUST, Map.of(), Map.of(day11, money("1000")),
                Map.of(day10, money("100")));

        assertTrue(result.days().get(9).hasData());
        assertEquals(money("100"), result.days().get(9).points());
        assertEquals(money("0"), result.days().get(9).mastercard());
        assertTrue(result.days().get(10).hasData());
        assertEquals(MonthlyDayRow.QrSource.REDEBAN, result.days().get(10).qrSource());
        assertEquals(2, result.daysWithData());
    }

    @Test
    void valuesOutsideTheMonthAreIgnored() {
        Map<LocalDate, DayFranchiseTotals> cards = Map.of(
                LocalDate.of(2026, 7, 31), day("9000000", "9000000", "9000000"),
                LocalDate.of(2026, 9, 1), day("8000000", "8000000", "8000000"));
        Map<LocalDate, BigDecimal> points = Map.of(LocalDate.of(2026, 7, 31), money("777"));

        MonthlyReconciliation result = of(AUGUST, cards, Map.of(), points);

        assertEquals(money("0"), result.totals().totalEds());
        assertEquals(money("0"), result.totals().qr());
        assertEquals(money("0"), result.totals().points());
        assertEquals(0, result.daysWithData());
    }

    @Test
    void theCommissionBaseExcludesAmexQrAndDawnVouchers() {
        DayFranchiseTotals mixed = new DayFranchiseTotals(money("1000000"), money("100"), money("200"), money("5"),
                money("6"), money("7"), money("2000000"));

        MonthlyReconciliation result = of(AUGUST, Map.of(LocalDate.of(2026, 8, 3), mixed), Map.of(), Map.of());

        assertEquals(money("300"), result.totals().totalEds());
        assertEquals(money("1000000"), result.totals().amex());
        assertEquals(money("2000300"), result.totals().totalWithQr());
        assertEquals(money("300"), result.commission().base());
        assertEquals(money("7.95"), result.commission().amount());
    }

    @Test
    void theCommissionRoundsHalfUpNotHalfEven() {
        Map<LocalDate, DayFranchiseTotals> cards = Map.of(
                LocalDate.of(2026, 8, 1), day("0.01", "0", "0"));

        MonthlyReconciliation result = MonthlyReconciliationCalculator.calculate(AUGUST, cards, Map.of(), Map.of(),
                new BigDecimal("0.5"));

        assertEquals(money("0.01"), result.commission().amount());
    }

    @Test
    void theRateMustBeAFractionBetweenZeroAndOne() {
        for (BigDecimal invalid : new BigDecimal[] {new BigDecimal("-0.01"), new BigDecimal("1.01"), null}) {
            ReconciliationValidationException error = assertThrows(ReconciliationValidationException.class,
                    () -> MonthlyReconciliationCalculator.calculate(AUGUST, Map.of(), Map.of(), Map.of(), invalid));
            assertEquals("commissionRate", error.field());
        }
        assertEquals(money("0"), MonthlyReconciliationCalculator.calculate(AUGUST, augustCards(), Map.of(), Map.of(),
                BigDecimal.ZERO).commission().amount());
        assertEquals(money("98868793"), MonthlyReconciliationCalculator.calculate(AUGUST, augustCards(), Map.of(),
                Map.of(), BigDecimal.ONE).commission().amount());
    }

    @Test
    void theRowsCannotBeModifiedByTheCaller() {
        List<MonthlyDayRow> days = august().days();

        assertThrows(UnsupportedOperationException.class, () -> days.add(days.get(0)));
    }
}
