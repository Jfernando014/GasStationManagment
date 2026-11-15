package com.edu.unicauca.gasstation.backend.reports.application;

import static com.edu.unicauca.gasstation.backend.reports.ReportTestData.DATE;
import static com.edu.unicauca.gasstation.backend.reports.ReportTestData.NEXT_DATE;
import static com.edu.unicauca.gasstation.backend.reports.ReportTestData.bd;
import static com.edu.unicauca.gasstation.backend.reports.ReportTestData.liveDay;
import static com.edu.unicauca.gasstation.backend.reports.ReportTestData.migratedDay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.edu.unicauca.gasstation.backend.reports.domain.models.BalanceReport;
import com.edu.unicauca.gasstation.backend.reports.domain.models.ReportDay;
import com.edu.unicauca.gasstation.backend.reports.exception.InvalidReportPeriodException;
import com.edu.unicauca.gasstation.backend.reports.exception.ReportNoDataException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests the executive balance period validation and aggregation.
 *
 * @author javiersolanop777
 */
class BalanceReportServiceTest {

    @Test
    void aggregatesDaysVariationsSellersAndLastCash() {
        BalanceReport report = service(List.of(liveDay(), migratedDay())).build(DATE, NEXT_DATE);

        assertEquals(2, report.days().size());
        assertEquals(1, report.liveDays());
        assertEquals(1, report.migratedDays());
        assertEquals(bd("1800"), report.totalGallons());
        assertEquals(bd("2200000"), report.totalDelivered());
        assertEquals(bd("900000"), report.totalCash());
        assertEquals(bd("107000"), report.totalBasket());
        assertEquals(bd("-531"), report.totalDiscrepancy());
        assertEquals(2, report.fuelVariations().size());
        assertEquals(1, report.sellers().size());
        assertEquals(bd("620"), report.sellers().get(0).gallons());
        assertEquals("MIGRATED", report.lastCash().journalStatus());
    }

    @Test
    void rejectsAPeriodWhoseStartIsAfterTheEnd() {
        InvalidReportPeriodException exception = assertThrows(InvalidReportPeriodException.class,
                () -> service(List.of()).build(NEXT_DATE, DATE));

        assertEquals("La fecha inicial no puede ser posterior a la fecha final", exception.getMessage());
    }

    @Test
    void rejectsPeriodsLongerThan366Days() {
        assertThrows(InvalidReportPeriodException.class,
                () -> service(List.of()).build(DATE, DATE.plusDays(366)));
    }

    @Test
    void failsWithTheExactNoDataMessageWhenThePeriodHasNoDays() {
        ReportNoDataException exception = assertThrows(ReportNoDataException.class,
                () -> service(List.of()).build(DATE, NEXT_DATE));

        assertEquals("No hay información suficiente para generar el balance de este período",
                exception.getMessage());
    }

    private static BalanceReportService service(List<ReportDay> days) {
        ReportDataService data = new ReportDataService(null, null, null, null) {
            @Override
            public List<ReportDay> findDays(LocalDate from, LocalDate to) {
                return days;
            }

            @Override
            public Optional<ReportDay> findDay(LocalDate date) {
                return days.stream().filter(day -> day.date().equals(date)).findFirst();
            }
        };
        return new BalanceReportService(data);
    }
}
