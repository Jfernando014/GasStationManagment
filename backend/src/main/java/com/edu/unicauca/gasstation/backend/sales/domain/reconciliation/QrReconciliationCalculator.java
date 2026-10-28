package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Compares the QR of Redeban with the QR declared in the planillas, per day and per vendor. */
public final class QrReconciliationCalculator {

    private QrReconciliationCalculator() {
    }

    public static QrReconciliationResult calculate(LocalDate date, List<VendorDeclaration> declarations,
                                                   Map<UUID, String> names, List<AssignedTransaction> redeban) {
        BigDecimal planillaTotal = declarations.stream().map(VendorDeclaration::planillaQr)
                .reduce(Money.ZERO, BigDecimal::add);
        boolean loaded = !redeban.isEmpty();
        BigDecimal redebanTotal = redeban.stream().map(t -> t.transaction().amount()).reduce(Money.ZERO, BigDecimal::add)
                .setScale(2);
        BigDecimal unassigned = redeban.stream().filter(t -> t.status() != AssignmentStatus.ASSIGNED)
                .map(t -> t.transaction().amount()).reduce(Money.ZERO, BigDecimal::add).setScale(2);
        Map<AssignmentStatus, Long> byStatus = new EnumMap<>(AssignmentStatus.class);
        for (AssignmentStatus status : AssignmentStatus.values()) {
            byStatus.put(status, redeban.stream().filter(t -> t.status() == status).count());
        }

        List<UnassignedTransaction> unassignedRows = redeban.stream()
                .filter(t -> t.status() != AssignmentStatus.ASSIGNED)
                .map(t -> new UnassignedTransaction(t.transaction().row(), t.transaction().time(),
                        t.transaction().amount().setScale(2), t.transaction().reference(), t.status()))
                .toList();

        List<VendorQrCheck> vendors = declarations.stream().map(d -> {
            BigDecimal assigned = redeban.stream()
                    .filter(t -> t.status() == AssignmentStatus.ASSIGNED && d.workerId().equals(t.workerId()))
                    .map(t -> t.transaction().amount()).reduce(Money.ZERO, BigDecimal::add).setScale(2);
            BigDecimal difference = assigned.subtract(d.planillaQr());
            return new VendorQrCheck(d.workerId(), names.get(d.workerId()), d.shiftCode(), assigned, d.planillaQr(),
                    difference, loaded && difference.signum() == 0);
        }).toList();

        BigDecimal qrTotal = loaded ? redebanTotal : planillaTotal;
        BigDecimal difference = redebanTotal.subtract(planillaTotal);
        return new QrReconciliationResult(date, loaded, redebanTotal, planillaTotal, qrTotal, difference,
                loaded && difference.signum() == 0, unassigned, loaded && unassigned.signum() == 0, byStatus, unassignedRows, vendors);
    }
}
