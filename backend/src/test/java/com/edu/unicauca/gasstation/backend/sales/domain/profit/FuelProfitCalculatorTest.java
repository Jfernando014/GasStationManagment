package com.edu.unicauca.gasstation.backend.sales.domain.profit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.ReconciliationValidationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FuelProfitCalculatorTest {

    private static final YearMonth AUGUST = YearMonth.of(2026, 8);

    @Test
    void referenceDateUsesTodayForCurrentMonthAndEndOfMonthForPastMonths() {
        assertEquals(
                LocalDate.of(2026, 8, 14),
                FuelProfitCalculator.referenceDate(AUGUST, LocalDate.of(2026, 8, 14)));
        assertEquals(
                LocalDate.of(2026, 8, 31),
                FuelProfitCalculator.referenceDate(AUGUST, LocalDate.of(2026, 9, 2)));
    }

    @Test
    void calculatesMonthlyProfitAcrossPriceChangesWithRealCardCommissionAndTargets() {
        FuelUnitValues motorDay1 = unit("16000", "14000", "200", "100"); // margin = 1700
        FuelUnitValues motorDay2 = unit("16500", "14200", "200", "100"); // margin = 2000
        FuelUnitValues dieselDay1 = unit("11000", "9500", "200", "100"); // margin = 1200

        Map<FuelType, List<FuelDayInput>> days = new EnumMap<>(FuelType.class);
        days.put(FuelType.MOTOR, List.of(
                new FuelDayInput(LocalDate.of(2026, 8, 1), new BigDecimal("100"), motorDay1),
                new FuelDayInput(LocalDate.of(2026, 8, 15), new BigDecimal("50"), motorDay2)));
        days.put(FuelType.DIESEL, List.of(
                new FuelDayInput(LocalDate.of(2026, 8, 1), new BigDecimal("200"), dieselDay1)));

        // Card sales = 25% of total sales (1,000,000 / 4,000,000); commission rate = 2.65% (0.0265)
        MonthlyFuelSales sales = new MonthlyFuelSales(
                2, days, new BigDecimal("1000000"), new BigDecimal("4000000"));

        Map<FuelType, FuelUnitValues> referenceValues = new EnumMap<>(FuelType.class);
        referenceValues.put(FuelType.MOTOR, motorDay2);
        referenceValues.put(FuelType.DIESEL, dieselDay1);
        referenceValues.put(FuelType.EXTRA, unit("20000", "18000", "200", "100"));
        referenceValues.put(FuelType.MAX_PRO, unit("12500", "11000", "200", "100"));

        Map<FuelType, BigDecimal> targets = Map.of(
                FuelType.MOTOR, new BigDecimal("300"),
                FuelType.DIESEL, new BigDecimal("400"),
                FuelType.EXTRA, new BigDecimal("100"),
                FuelType.MAX_PRO, new BigDecimal("100"));

        FuelProfitReport report = FuelProfitCalculator.calculate(
                AUGUST,
                LocalDate.of(2026, 8, 31),
                sales,
                referenceValues,
                targets,
                FuelProfitCardSetting.real(AUGUST),
                new BigDecimal("0.0265"));

        assertEquals("2026-08", report.month());
        assertEquals(31, report.daysInMonth());
        assertEquals(2, report.daysWithData());
        assertEquals(0, new BigDecimal("25").compareTo(report.card().appliedPercentage()));
        assertEquals(0, new BigDecimal("25").compareTo(report.card().realPercentage()));

        FuelProfit motor = fuelOf(report, FuelType.MOTOR);
        assertTrue(motor.complete());
        assertEquals(new BigDecimal("150"), motor.gallons());
        // sales = 100*16000 + 50*16500 = 2,425,000.00
        assertEquals(new BigDecimal("2425000.00"), motor.sales());
        // realProfit = 100*1700 + 50*2000 = 270,000.00
        assertEquals(new BigDecimal("270000.00"), motor.realProfit());
        // commission = 2,425,000 * 25% * 0.0265 = 16,065.625 -> 16,065.63
        assertEquals(new BigDecimal("16065.63"), motor.commission());
        assertEquals(new BigDecimal("253934.37"), motor.profitAfterCommission());
        // targetProfit = 300 * 2000 (reference margin on Aug 31) = 600,000.00
        assertEquals(new BigDecimal("600000.00"), motor.targetProfit());
        assertEquals(new BigDecimal("50.00"), motor.compliancePercentage());

        FuelProfit diesel = fuelOf(report, FuelType.DIESEL);
        // sales = 200*11000 = 2,200,000.00; profit = 200*1200 = 240,000.00
        // commission = 2,200,000 * 25% * 0.0265 = 14,575.00
        assertEquals(new BigDecimal("2200000.00"), diesel.sales());
        assertEquals(new BigDecimal("240000.00"), diesel.realProfit());
        assertEquals(new BigDecimal("14575.00"), diesel.commission());

        assertTrue(report.totals().complete());
        assertTrue(report.totals().incompleteFuels().isEmpty());
        assertTrue(report.totals().fuelsWithoutTarget().isEmpty());
        assertEquals(new BigDecimal("350"), report.totals().gallons());
        assertEquals(new BigDecimal("4625000.00"), report.totals().sales());
        assertEquals(new BigDecimal("510000.00"), report.totals().realProfit());
        assertEquals(new BigDecimal("30640.63"), report.totals().commission());
        assertEquals(new BigDecimal("479359.37"), report.totals().profitAfterCommission());
    }

    @Test
    void appliesManualCardPercentageAndValidatesCardSettingBounds() {
        Map<FuelType, List<FuelDayInput>> days = Map.of(
                FuelType.MOTOR, List.of(new FuelDayInput(
                        LocalDate.of(2026, 8, 5),
                        new BigDecimal("100"),
                        unit("15000", "13000", "200", "100"))));

        // Even with zero total sales (real% = null), MANUAL 40% still calculates card commission
        MonthlyFuelSales sales = new MonthlyFuelSales(1, days, BigDecimal.ZERO, BigDecimal.ZERO);
        FuelProfitCardSetting manual = new FuelProfitCardSetting(
                "2026-08", CardPercentageMode.MANUAL, new BigDecimal("40"));

        FuelProfitReport report = FuelProfitCalculator.calculate(
                AUGUST, LocalDate.of(2026, 8, 31), sales, Map.of(), Map.of(), manual, new BigDecimal("0.025"));

        FuelProfit motor = fuelOf(report, FuelType.MOTOR);
        assertNull(report.card().realPercentage());
        assertEquals(0, new BigDecimal("40").compareTo(report.card().appliedPercentage()));
        // 1,500,000 * 40% * 0.025 = 15,000.00
        assertEquals(new BigDecimal("15000.00"), motor.commission());
        assertEquals(new BigDecimal("155000.00"), motor.profitAfterCommission());

        assertThrows(ReconciliationValidationException.class,
                () -> new FuelProfitCardSetting("2026-08", CardPercentageMode.MANUAL, null));
        assertThrows(ReconciliationValidationException.class,
                () -> new FuelProfitCardSetting("2026-08", CardPercentageMode.MANUAL, new BigDecimal("-0.01")));
        assertThrows(ReconciliationValidationException.class,
                () -> new FuelProfitCardSetting("2026-08", CardPercentageMode.MANUAL, new BigDecimal("100.01")));
        // REAL mode ignores any provided manual percentage
        assertNull(new FuelProfitCardSetting("2026-08", CardPercentageMode.REAL, new BigDecimal("20")).manualPercentage());
    }

    @Test
    void propagatesNullAndListsMissingDatumWhenAnyCostIsMissing() {
        FuelUnitValues missingPurchase = new FuelUnitValues(
                new BigDecimal("16000"), null, new BigDecimal("200"), new BigDecimal("100"));
        Map<FuelType, List<FuelDayInput>> days = Map.of(
                FuelType.MOTOR, List.of(new FuelDayInput(LocalDate.of(2026, 8, 3), new BigDecimal("80"), missingPurchase)));
        MonthlyFuelSales sales = new MonthlyFuelSales(
                1, days, new BigDecimal("500000"), new BigDecimal("2000000"));

        FuelProfitReport report = FuelProfitCalculator.calculate(
                AUGUST, LocalDate.of(2026, 8, 31), sales, Map.of(), Map.of(),
                FuelProfitCardSetting.real(AUGUST), new BigDecimal("0.0265"));

        FuelProfit motor = fuelOf(report, FuelType.MOTOR);
        assertFalse(motor.complete());
        assertEquals(new BigDecimal("1280000.00"), motor.sales());
        assertNull(motor.realProfit());
        assertNull(motor.profitAfterCommission());
        assertEquals(List.of(new MissingData(MissingDatum.PURCHASE_COST, LocalDate.of(2026, 8, 3))), motor.missingData());

        assertFalse(report.totals().complete());
        assertEquals(List.of(FuelType.MOTOR), report.totals().incompleteFuels());
        assertNull(report.totals().realProfit());
        assertNull(report.totals().profitAfterCommission());
        assertNull(report.totals().targetGallons());
        assertEquals(4, report.totals().fuelsWithoutTarget().size());
    }

    private static FuelUnitValues unit(String sale, String purchase, String freight, String financial) {
        return new FuelUnitValues(
                new BigDecimal(sale), new BigDecimal(purchase),
                new BigDecimal(freight), new BigDecimal(financial));
    }

    private static FuelProfit fuelOf(FuelProfitReport report, FuelType type) {
        return report.fuels().stream()
                .filter(f -> f.fuelType() == type)
                .findFirst()
                .orElseThrow();
    }
}
