package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Result of the QR reconciliation of a day.
 *
 * @param redebanLoaded    false when no CSV was imported for the date; then only the planilla total is known
 * @param qrTotal          QR of the day: the whole Redeban total when loaded, otherwise the sum of the planillas
 * @param perVendorReliable true only when every row was assigned; otherwise the vendor comparison is partial
 * @param byStatus         number of Redeban rows by assignment status
 * @param unassigned       the rows that were not attributed to a vendor, with the reason
 */
public record QrReconciliationResult(
        LocalDate date,
        boolean redebanLoaded,
        BigDecimal redebanTotal,
        BigDecimal planillaTotal,
        BigDecimal qrTotal,
        BigDecimal difference,
        boolean matchesDay,
        BigDecimal unassignedTotal,
        boolean perVendorReliable,
        Map<AssignmentStatus, Long> byStatus,
        List<UnassignedTransaction> unassigned,
        List<VendorQrCheck> vendors) {
}
