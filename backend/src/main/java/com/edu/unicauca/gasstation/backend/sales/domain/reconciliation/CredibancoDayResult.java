package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Result of the day (HU-34 #3 and #4).
 *
 * @param allMatch      every vendor with data matches his planilla (false when there is no vendor)
 * @param franchises    franchise totals of the day; its {@code qr} is the QR of the day, whether counted or not
 * @param qrAdded       QR is added to the accounting total only when every vendor matches
 * @param totalEds      Mastercard + Visa of the day, without QR
 * @param monthToDate   accumulated from day 1 of the month up to and including this date
 * @param commissionBase TOTAL EDS accumulated in the month: QR never pays commission
 */
public record CredibancoDayResult(
        LocalDate date,
        List<VendorCredibancoCheck> vendors,
        boolean allMatch,
        DayFranchiseTotals franchises,
        boolean qrAdded,
        BigDecimal totalEds,
        AccountingReport accounting,
        PeriodTotals monthToDate,
        PeriodTotals previousMonth,
        BigDecimal commissionRate,
        BigDecimal commissionBase,
        BigDecimal commission) {
}
