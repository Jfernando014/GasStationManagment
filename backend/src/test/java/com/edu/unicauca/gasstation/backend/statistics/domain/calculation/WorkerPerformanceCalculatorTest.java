package com.edu.unicauca.gasstation.backend.statistics.domain.calculation;

import com.edu.unicauca.gasstation.backend.statistics.SellerPerformanceTestData;
import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.WorkerPerformance.PerformanceWeekday;
import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.WorkerPerformance.Report;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleAmounts;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistory;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleHistory.EffectiveRole;
import com.edu.unicauca.gasstation.backend.statistics.domain.models.ShiftSaleIdentity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static com.edu.unicauca.gasstation.backend.statistics.SellerPerformanceTestData.A;
import static com.edu.unicauca.gasstation.backend.statistics.SellerPerformanceTestData.B;
import static com.edu.unicauca.gasstation.backend.statistics.SellerPerformanceTestData.journal;
import static com.edu.unicauca.gasstation.backend.statistics.SellerPerformanceTestData.sale;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkerPerformanceCalculatorTest {
    private static final UUID C = UUID.fromString("cccccccc-cccc-4ccc-8ccc-cccccccccccc");
    private final WorkerPerformanceCalculator calculator = new WorkerPerformanceCalculator();

    @Test
    void emptySalesReturnsEmptyReportWithEightOrderedWeekdayBuckets() {
        Report report = calculator.calculate(A, List.of(), date -> false);

        assertThat(report.periodFrom()).isNull();
        assertThat(report.periodTo()).isNull();
        assertThat(report.closedShifts().total()).isZero();
        assertThat(report.closedShifts().asHolder()).isZero();
        assertThat(report.closedShifts().asSupport()).isZero();
        assertThat(report.maxSale()).isNull();
        assertThat(report.byWeekday())
                .extracting(row -> row.weekday())
                .containsExactly(PerformanceWeekday.values());
        assertThat(report.byWeekday()).allSatisfy(row -> {
            assertThat(row.asHolder().shifts()).isZero();
            assertThat(row.asHolder().averageGallons()).isNull();
            assertThat(row.asHolder().performancePercent()).isNull();
            assertThat(row.asSupport().shifts()).isZero();
            assertThat(row.asSupport().averageGallons()).isNull();
            assertThat(row.asSupport().performancePercent()).isNull();
        });
    }

    @Test
    void unrelatedStationSalesReturnEmptyReportWithoutConsultingHolidays() {
        AtomicInteger calls = new AtomicInteger();
        Report report = calculator.calculate(
                A,
                List.of(sale(B, "100", "5000")),
                date -> {
                    calls.incrementAndGet();
                    return true;
                });

        assertThat(report.closedShifts().total()).isZero();
        assertThat(calls.get()).isZero();
    }

    @Test
    void classifiesHolidaysIntoHolidayBucketAndSeparatesHolderFromSupport() {
        LocalDate mon1 = LocalDate.of(2026, 8, 3);
        LocalDate holidayFriday = LocalDate.of(2026, 8, 7);
        LocalDate mon2 = LocalDate.of(2026, 8, 10);
        Set<LocalDate> holidays = Set.of(holidayFriday);

        List<ShiftSaleHistory> sales = List.of(
                sale(mon1, A, "DIA", EffectiveRole.TITULAR, "120", "0"),
                sale(mon1, B, "DIA", EffectiveRole.TITULAR, "80", "0"),
                sale(holidayFriday, A, "DIA", EffectiveRole.APOYO, "90", "0"),
                sale(holidayFriday, B, "DIA", EffectiveRole.APOYO, "110", "0"),
                sale(mon2, A, "DIA", EffectiveRole.TITULAR, "100", "0"));

        Report report = calculator.calculate(A, sales, holidays::contains);

        assertThat(report.periodFrom()).isEqualTo(mon1);
        assertThat(report.periodTo()).isEqualTo(mon2);
        assertThat(report.closedShifts().total()).isEqualTo(3);
        assertThat(report.closedShifts().asHolder()).isEqualTo(2);
        assertThat(report.closedShifts().asSupport()).isEqualTo(1);
        assertThat(report.maxSale().gallons()).isEqualByComparingTo("120.00");
        assertThat(report.maxSale().date()).isEqualTo(mon1);
        assertThat(report.maxSale().shiftCode()).isEqualTo("DIA");

        var monday = report.byWeekday().get(0);
        assertThat(monday.weekday()).isEqualTo(PerformanceWeekday.MONDAY);
        assertThat(monday.asHolder().shifts()).isEqualTo(2);
        assertThat(monday.asHolder().averageGallons()).isEqualByComparingTo("110.00");
        assertThat(monday.asHolder().performancePercent()).isEqualByComparingTo("110.00");
        assertThat(monday.asSupport().shifts()).isZero();

        var friday = report.byWeekday().get(4);
        assertThat(friday.weekday()).isEqualTo(PerformanceWeekday.FRIDAY);
        assertThat(friday.asSupport().shifts()).isZero();

        var holiday = report.byWeekday().get(7);
        assertThat(holiday.weekday()).isEqualTo(PerformanceWeekday.HOLIDAY);
        assertThat(holiday.asHolder().shifts()).isZero();
        assertThat(holiday.asSupport().shifts()).isEqualTo(1);
        assertThat(holiday.asSupport().averageGallons()).isEqualByComparingTo("90.00");
        assertThat(holiday.asSupport().performancePercent()).isEqualByComparingTo("90.00");
    }

    @Test
    void comparesEachShiftCodeAgainstSameCodeCohortWithoutIntermediateRoundingDrift() {
        LocalDate tue1 = LocalDate.of(2026, 8, 4);
        LocalDate tue2 = LocalDate.of(2026, 8, 11);

        List<ShiftSaleHistory> sales = List.of(
                // Tuesday DIA station cohort: (101 + 100 + 100) / 3 = 100.3333... (non-terminating decimal)
                sale(tue1, A, "DIA", EffectiveRole.TITULAR, "101", "0"),
                sale(tue1, B, "DIA", EffectiveRole.TITULAR, "100", "0"),
                sale(tue1, C, "DIA", EffectiveRole.TITULAR, "100", "0"),
                // Tuesday NOCHE station cohort: (45 + 55) / 2 = 50
                sale(tue2, A, "NOCHE", EffectiveRole.TITULAR, "45", "0"),
                sale(tue2, B, "NOCHE", EffectiveRole.TITULAR, "55", "0"));

        Report report = calculator.calculate(A, sales, date -> false);
        var tuesday = report.byWeekday().get(1);

        assertThat(tuesday.weekday()).isEqualTo(PerformanceWeekday.TUESDAY);
        assertThat(tuesday.asHolder().shifts()).isEqualTo(2);
        // Average: (101 + 45) / 2 = 73.00
        assertThat(tuesday.asHolder().averageGallons()).isEqualByComparingTo("73.00");
        // Exact expected: (301/3) + 50 = 451/3 -> ratio (146 * 100) / (451/3) = 43800 / 451 = 97.12
        assertThat(tuesday.asHolder().performancePercent()).isEqualByComparingTo("97.12");
    }

    @Test
    void sumsAllFourFuelProductsAndBreaksMaxSaleTiesByDateThenWorkedCode() {
        LocalDate d1 = LocalDate.of(2026, 8, 5);
        LocalDate d2 = LocalDate.of(2026, 8, 12);

        ShiftSaleHistory multiFuelOld = new ShiftSaleHistory(
                journal(d1),
                new ShiftSaleIdentity(d1, A.toString(), "DIA"),
                A,
                "Recorded A",
                "DIA",
                EffectiveRole.TITULAR,
                new ShiftSaleAmounts(
                        new BigDecimal("40.25"),
                        new BigDecimal("50.25"),
                        new BigDecimal("30.00"),
                        new BigDecimal("29.50"),
                        new BigDecimal("500000"),
                        new BigDecimal("500000"),
                        new BigDecimal("500000")));
        ShiftSaleHistory tieLaterDia = sale(d2, A, "DIA", EffectiveRole.TITULAR, "150", "10");
        ShiftSaleHistory tieLaterNoche = sale(d2, A, "NOCHE", EffectiveRole.TITULAR, "150", "10");

        Report report = calculator.calculate(A, List.of(multiFuelOld, tieLaterDia, tieLaterNoche), date -> false);

        assertThat(report.maxSale().gallons()).isEqualByComparingTo("150.00");
        assertThat(report.maxSale().date()).isEqualTo(d2);
        assertThat(report.maxSale().shiftCode()).isEqualTo("NOCHE");
    }

    @Test
    void zeroStationBaselineLeavesAverageDefinedAndPerformancePercentNull() {
        Report report = calculator.calculate(
                A,
                List.of(sale(A, "0", "100"), sale(B, "0", "200")),
                date -> false);

        var monday = report.byWeekday().get(0);
        assertThat(monday.asHolder().shifts()).isEqualTo(1);
        assertThat(monday.asHolder().averageGallons()).isEqualByComparingTo("0.00");
        assertThat(monday.asHolder().performancePercent()).isNull();
        assertThat(report.maxSale().gallons()).isEqualByComparingTo("0.00");
    }

    @Test
    void rejectsNullOrZeroWorkerUuid() {
        assertThatThrownBy(() -> calculator.calculate(null, List.of(), date -> false))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> calculator.calculate(new UUID(0, 0), List.of(), date -> false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("zero");
        assertThat(SellerPerformanceTestData.A).isNotNull();
    }
}
