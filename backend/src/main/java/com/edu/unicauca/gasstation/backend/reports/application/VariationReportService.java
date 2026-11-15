package com.edu.unicauca.gasstation.backend.reports.application;

import com.edu.unicauca.gasstation.backend.reports.domain.models.ReportDay;
import com.edu.unicauca.gasstation.backend.reports.domain.models.VariationReport;
import com.edu.unicauca.gasstation.backend.reports.exception.ReportNoDataException;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/**
 * Builds the VARIACIÓN report of one operational day.
 *
 * @author javiersolanop777
 */
@Service
public class VariationReportService {

    private static final String NO_DATA = "No hay información suficiente para generar el reporte de esta fecha";

    private final ReportDataService reportDataService;

    public VariationReportService(ReportDataService reportDataService) {
        this.reportDataService = reportDataService;
    }

    public VariationReport build(LocalDate date) {
        ReportDay day = reportDataService.findDay(date)
                .orElseThrow(() -> new ReportNoDataException(NO_DATA));
        if (day.variations().isEmpty()) {
            throw new ReportNoDataException(NO_DATA);
        }
        return new VariationReport(date, day.source(), day.variations(), day.alertCount());
    }
}
