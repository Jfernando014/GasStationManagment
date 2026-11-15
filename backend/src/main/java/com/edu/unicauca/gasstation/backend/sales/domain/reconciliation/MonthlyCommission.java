package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Credibanco commission of the month: {@code rate x base}, rounded HALF_UP to 2 decimals. QR and Amex never pay it.
 *
 * @param rate fraction, e.g. 0.026500 for 2,65 %, effective on {@code rateDate}
 * @param rateDate the date whose effective rate was used (the last day of the month)
 * @param base TOTAL EDS of the month (Mastercard + Visa)
 * @param amount commission in COP
 */
public record MonthlyCommission(BigDecimal rate, LocalDate rateDate, BigDecimal base, BigDecimal amount) {
}
