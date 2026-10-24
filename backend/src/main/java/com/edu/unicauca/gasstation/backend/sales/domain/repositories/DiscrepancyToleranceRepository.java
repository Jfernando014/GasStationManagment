package com.edu.unicauca.gasstation.backend.sales.domain.repositories;

import com.edu.unicauca.gasstation.backend.sales.domain.models.DiscrepancyToleranceConfig;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Domain repository port for discrepancy tolerance configuration.
 */
public interface DiscrepancyToleranceRepository {

    Optional<DiscrepancyToleranceConfig> findEffectiveTolerance(LocalDate date);

    Optional<DiscrepancyToleranceConfig> findByEffectiveDate(LocalDate effectiveDate);

    DiscrepancyToleranceConfig save(DiscrepancyToleranceConfig config);

    List<DiscrepancyToleranceConfig> findAllHistory();

    List<DiscrepancyToleranceConfig> findScheduledAfter(LocalDate date);
}
