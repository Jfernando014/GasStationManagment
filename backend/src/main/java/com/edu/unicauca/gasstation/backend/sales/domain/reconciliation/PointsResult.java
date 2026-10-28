package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * @param declared    sum of the redemptions of every planilla
 * @param pointsTotal day total of Puntos Colombia; null when not loaded
 * @param difference  pointsTotal - declared; null when there is no total to compare
 * @param source      where the total came from (MANUAL, EXCEL) or null
 */
public record PointsResult(LocalDate date, BigDecimal declared, BigDecimal pointsTotal, BigDecimal difference,
                           PointsStatus status, String source, List<VendorRedemption> vendors) {

    /** Redemptions declared by one vendor. */
    public record VendorRedemption(UUID workerId, String workerName, BigDecimal redemptions) {
    }
}
