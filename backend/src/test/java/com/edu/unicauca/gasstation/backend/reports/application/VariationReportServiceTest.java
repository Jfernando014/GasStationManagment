package com.edu.unicauca.gasstation.backend.reports.application;

import static com.edu.unicauca.gasstation.backend.reports.ReportTestData.DATE;
import static com.edu.unicauca.gasstation.backend.reports.ReportTestData.liveDay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.edu.unicauca.gasstation.backend.reports.domain.models.ReportDay;
import com.edu.unicauca.gasstation.backend.reports.domain.models.VariationReport;
import com.edu.unicauca.gasstation.backend.reports.exception.ReportNoDataException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests the VARIACIÓN report construction and its no-data rule.
 *
 * @author javiersolanop777
 */
class VariationReportServiceTest {

    @Test
    void buildsTheVariationReportFromTheNormalizedDay() {
        ReportDataService data = new ReportDataService(null, null, null, null) {
            @Override
            public Optional<ReportDay> findDay(java.time.LocalDate date) {
                return Optional.of(liveDay());
            }
        };

        VariationReport report = new VariationReportService(data).build(DATE);

        assertEquals(DATE, report.date());
        assertEquals(ReportDay.Source.LIVE, report.source());
        assertEquals(1, report.variations().size());
        assertEquals(2, report.alertCount());
    }

    @Test
    void failsWithTheExactNoDataMessageWhenTheDayDoesNotExist() {
        ReportDataService data = new ReportDataService(null, null, null, null) {
            @Override
            public Optional<ReportDay> findDay(java.time.LocalDate date) {
                return Optional.empty();
            }
        };

        ReportNoDataException exception = assertThrows(ReportNoDataException.class,
                () -> new VariationReportService(data).build(DATE));

        assertEquals("No hay información suficiente para generar el reporte de esta fecha", exception.getMessage());
    }

    @Test
    void failsWithTheExactNoDataMessageWhenTheDayHasNoVariations() {
        ReportDataService data = new ReportDataService(null, null, null, null) {
            @Override
            public Optional<ReportDay> findDay(java.time.LocalDate date) {
                ReportDay day = liveDay();
                return Optional.of(new ReportDay(day.date(), day.source(), day.totalGallons(),
                        day.totalDelivered(), day.totalCash(), day.totalBasket(), day.totalDiscrepancy(),
                        day.fuelSales(), day.vendors(), java.util.List.of(), day.cash(), day.alertCount()));
            }
        };

        assertThrows(ReportNoDataException.class, () -> new VariationReportService(data).build(DATE));
    }
}
