package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

import java.math.BigDecimal;

/** "Reporte contable": franchise amounts moved by the dawn vouchers; QR and total only when every vendor matches. */
public record AccountingReport(BigDecimal amex, BigDecimal mastercard, BigDecimal visa, BigDecimal qr,
                               BigDecimal total) {
}
