package com.edu.unicauca.gasstation.backend.reports.domain.services;

import com.edu.unicauca.gasstation.backend.reports.domain.models.ReportModelExample;
import java.util.Optional;

/**
 * Example domain service interface for reports module.
 */
public interface ReportServiceExample {

    Optional<ReportModelExample> findById(Long id);

    ReportModelExample save(ReportModelExample model);
}
