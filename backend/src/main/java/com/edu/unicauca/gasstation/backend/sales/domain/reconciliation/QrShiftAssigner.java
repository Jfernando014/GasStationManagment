package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

import java.util.List;
import java.util.UUID;

/** Attributes each QR row to the vendor whose shift covers the hour of the row (HU-35). */
public final class QrShiftAssigner {

    private QrShiftAssigner() {
    }

    public static List<AssignedTransaction> assign(List<RedebanTransaction> transactions, List<VendorShift> shifts) {
        return transactions.stream().map(t -> {
            if (t.time() == null) {
                return new AssignedTransaction(t, null, AssignmentStatus.NO_TIME);
            }
            List<UUID> candidates = shifts.stream()
                    .filter(s -> s.hours().contains(t.time().getHour()))
                    .map(VendorShift::workerId)
                    .toList();
            if (candidates.size() == 1) {
                return new AssignedTransaction(t, candidates.get(0), AssignmentStatus.ASSIGNED);
            }
            return new AssignedTransaction(t, null,
                    candidates.isEmpty() ? AssignmentStatus.NO_SHIFT : AssignmentStatus.AMBIGUOUS);
        }).toList();
    }
}
