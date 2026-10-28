package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** HU-35 #1 and #2: redemptions of the planillas against the day total of Puntos Colombia. */
public final class PointsReconciliation {

    private PointsReconciliation() {
    }

    public static PointsResult compare(LocalDate date, List<VendorDeclaration> declarations, Map<UUID, String> names,
                                       BigDecimal pointsTotal, String source) {
        BigDecimal declared = declarations.stream().map(VendorDeclaration::planillaRedemptions)
                .reduce(Money.ZERO, BigDecimal::add);
        BigDecimal total = pointsTotal == null ? null : Money.amount("pointsTotal", pointsTotal);
        List<PointsResult.VendorRedemption> vendors = declarations.stream()
                .map(d -> new PointsResult.VendorRedemption(d.workerId(), names.get(d.workerId()), d.planillaRedemptions()))
                .toList();

        PointsStatus status;
        BigDecimal difference = total == null ? null : total.subtract(declared);
        if (declared.signum() == 0 && (total == null || total.signum() == 0)) {
            status = PointsStatus.COMPLETED_ZERO;
            difference = Money.ZERO;
            total = total == null ? Money.ZERO : total;
        } else if (total == null) {
            status = PointsStatus.PENDING_TOTAL;
        } else {
            status = difference.signum() == 0 ? PointsStatus.MATCH : PointsStatus.DIFFERENCE;
        }
        return new PointsResult(date, declared, total, difference, status, source, vendors);
    }
}
