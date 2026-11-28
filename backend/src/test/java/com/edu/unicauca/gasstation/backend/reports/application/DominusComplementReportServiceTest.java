package com.edu.unicauca.gasstation.backend.reports.application;

import static com.edu.unicauca.gasstation.backend.reports.ReportTestData.DATE;
import static com.edu.unicauca.gasstation.backend.reports.ReportTestData.bd;
import static com.edu.unicauca.gasstation.backend.reports.ReportTestData.liveDay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.edu.unicauca.gasstation.backend.reports.domain.models.DominusComplementReport;
import com.edu.unicauca.gasstation.backend.reports.domain.models.ReportDay;
import com.edu.unicauca.gasstation.backend.reports.exception.ReportNoDataException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests the COMPLEMENTO DOMINUS totals and its no-data rule.
 *
 * @author javiersolanop777
 */
class DominusComplementReportServiceTest {

    @Test
    void buildsTheComplementFromTheNormalizedDay() {
        ReportDataService data = day(liveDay());

        DominusComplementReport report = new DominusComplementReportService(data).build(DATE);

        assertEquals(DATE, report.date());
        assertEquals(1, report.vendors().size());
        assertEquals(bd("310"), report.totalGallons());
        assertEquals(bd("569469"), report.totalDelivered());
        assertEquals(bd("570000"), report.totalDominusTirilla());
        assertEquals(bd("-531"), report.totalDiscrepancy());
        assertEquals(bd("900000"), report.totalCash());
    }

    @Test
    void failsWhenThereAreNoVendorsAndNoCash() {
        ReportDataService data = day(new ReportDay(DATE, ReportDay.Source.LIVE, bd("0"), bd("0"),
                null, bd("0"), bd("0"), List.of(), List.of(), List.of(), null, 0));

        ReportNoDataException exception = assertThrows(ReportNoDataException.class,
                () -> new DominusComplementReportService(data).build(DATE));

        assertEquals("No hay información suficiente para generar el reporte de esta fecha", exception.getMessage());
    }

    private static ReportDataService day(ReportDay day) {
        return new ReportDataService(null, null, null, null) {
            @Override
            public Optional<ReportDay> findDay(java.time.LocalDate date) {
                return Optional.of(day);
            }
        };
    }
}
