package com.edu.unicauca.gasstation.backend.reports.application;

import com.edu.unicauca.gasstation.backend.reports.domain.models.DominusComplementReport;
import com.edu.unicauca.gasstation.backend.reports.domain.models.ReportDay;
import com.edu.unicauca.gasstation.backend.reports.exception.ReportNoDataException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/**
 * Builds the COMPLEMENTO DOMINUS report of one operational day.
 *
 * @author javiersolanop777
 */
@Service
public class DominusComplementReportService {

    private static final String NO_DATA = "No hay información suficiente para generar el reporte de esta fecha";

    private final ReportDataService reportDataService;

    public DominusComplementReportService(ReportDataService reportDataService) {
        this.reportDataService = reportDataService;
    }

    public DominusComplementReport build(LocalDate date) {
        ReportDay day = reportDataService.findDay(date)
                .orElseThrow(() -> new ReportNoDataException(NO_DATA));
        if (day.vendors().isEmpty() && day.cash() == null) {
            throw new ReportNoDataException(NO_DATA);
        }
        BigDecimal delivered = sum(day.vendors().stream().map(ReportDay.VendorRow::totalDelivered).toList());
        BigDecimal tirilla = sum(day.vendors().stream().map(ReportDay.VendorRow::dominusTirilla).toList());
        BigDecimal discrepancy = sum(day.vendors().stream().map(ReportDay.VendorRow::discrepancy).toList());
        return new DominusComplementReport(date, day.source(), day.cash(), day.vendors(),
                delivered, tirilla, discrepancy, day.totalCash(), day.totalBasket());
    }

    private static BigDecimal sum(java.util.List<BigDecimal> values) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            total = total.add(value == null ? BigDecimal.ZERO : value);
        }
        return total;
    }
}
