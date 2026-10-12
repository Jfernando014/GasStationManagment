package com.edu.unicauca.gasstation.backend.sales.domain.repositories;

import com.edu.unicauca.gasstation.backend.sales.domain.calculation.DistributionCalculation.Assignment;
import java.time.YearMonth;
import java.util.List;

/** Awaiting a public shifts assignment-query contract. Never reads internal shifts repositories. */
public interface SalesAssignmentReadPort {
    List<Assignment> findAssignments(YearMonth month);
}
