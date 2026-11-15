package com.edu.unicauca.gasstation.backend.reports.domain.models;

import java.time.LocalDate;
import java.util.List;

/**
 * Data of the VARIACIÓN Excel file of one operational day.
 *
 * @author javiersolanop777
 */
public record VariationReport(
        LocalDate date,
        ReportDay.Source source,
        List<ReportDay.FuelVariation> variations,
        int alertCount
) {

    public VariationReport {
        variations = variations != null ? List.copyOf(variations) : List.of();
    }
}
