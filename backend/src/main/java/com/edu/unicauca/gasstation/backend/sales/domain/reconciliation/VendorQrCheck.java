package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

import java.math.BigDecimal;
import java.util.UUID;

/** QR assigned to a vendor from Redeban against the QR of his planilla. difference = redeban - planilla. */
public record VendorQrCheck(UUID workerId, String workerName, String shiftCode, BigDecimal redeban,
                            BigDecimal planilla, BigDecimal difference, boolean matches) {
}
