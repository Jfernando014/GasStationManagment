package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

import java.math.BigDecimal;
import java.util.UUID;

/** HU-34 #1/#2: vouchers of a vendor against the Credibanco of his planilla. difference = vouchers - planilla. */
public record VendorCredibancoCheck(
        UUID workerId,
        String workerName,
        String shiftCode,
        BigDecimal vouchersTotal,
        BigDecimal planillaCredibanco,
        BigDecimal difference,
        boolean matches) {
}
