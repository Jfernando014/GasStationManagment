package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * HU-34 arithmetic, taken from the REPORTE CREDIBANCO workbook:
 * <ul>
 *   <li>TOTAL EDS = Mastercard + Visa (no Amex, no QR).</li>
 *   <li>Accounting franchise = franchise + dawn of the previous day - dawn of the day.</li>
 *   <li>Accounting total = Amex + Mastercard + Visa (+ QR only when every vendor matches).</li>
 *   <li>Commission = rate x TOTAL EDS accumulated in the month, rounded HALF_UP to 2 decimals.</li>
 * </ul>
 * Pure Java: no Spring, no database.
 */
public final class CredibancoReconciliationCalculator {

    private CredibancoReconciliationCalculator() {
    }

    /**
     * @param dayQr         QR of the day (Redeban when loaded, otherwise the sum of the planillas)
     * @param previousDay   franchise totals of the previous calendar day (its dawn vouchers belong to this report)
     * @param monthBefore   daily totals already stored for the days of the month before {@code date}
     * @param previousMonth opening figures of the previous month
     * @param commissionRate fraction, e.g. 0.0265 for 2.65 %
     */
    public static CredibancoDayResult calculate(LocalDate date, List<VendorDeclaration> declarations,
                                                Map<UUID, String> names, BigDecimal dayQr,
                                                DayFranchiseTotals previousDay,
                                                Map<LocalDate, DayFranchiseTotals> monthBefore,
                                                PeriodTotals previousMonth, BigDecimal commissionRate) {
        if (commissionRate == null || commissionRate.signum() < 0 || commissionRate.compareTo(BigDecimal.ONE) > 0) {
            throw new ReconciliationValidationException("commissionRate", "La comisión debe estar entre 0 y 1");
        }

        List<VendorCredibancoCheck> checks = declarations.stream().map(d -> {
            BigDecimal difference = d.vouchersTotal().subtract(d.planillaCredibanco());
            return new VendorCredibancoCheck(d.workerId(), names.get(d.workerId()), d.shiftCode(),
                    d.vouchersTotal(), d.planillaCredibanco(), difference, difference.signum() == 0);
        }).toList();
        boolean allMatch = !checks.isEmpty() && checks.stream().allMatch(VendorCredibancoCheck::matches);

        DayFranchiseTotals day = DayFranchiseTotals.of(declarations, dayQr);
        BigDecimal accountingAmex = day.amex().add(previousDay.amexDawn()).subtract(day.amexDawn());
        BigDecimal accountingMastercard = day.mastercard().add(previousDay.mastercardDawn())
                .subtract(day.mastercardDawn());
        BigDecimal accountingVisa = day.visa().add(previousDay.visaDawn()).subtract(day.visaDawn());
        BigDecimal accountingQr = allMatch ? day.qr() : Money.ZERO;
        AccountingReport accounting = new AccountingReport(accountingAmex, accountingMastercard, accountingVisa,
                accountingQr, accountingAmex.add(accountingMastercard).add(accountingVisa).add(accountingQr));

        YearMonth month = YearMonth.from(date);
        DayFranchiseTotals accumulated = day;
        for (Map.Entry<LocalDate, DayFranchiseTotals> entry : monthBefore.entrySet()) {
            if (entry.getKey().isBefore(date) && YearMonth.from(entry.getKey()).equals(month)) {
                accumulated = accumulated.plus(entry.getValue());
            }
        }
        PeriodTotals monthToDate = PeriodTotals.of(accumulated.mastercard(), accumulated.visa(),
                accumulated.amex(), accumulated.qr(), "DAILY_DATA");
        BigDecimal commission = monthToDate.totalEds().multiply(commissionRate).setScale(2, RoundingMode.HALF_UP);

        return new CredibancoDayResult(date, checks, allMatch, day, allMatch, day.totalEds(), accounting,
                monthToDate, previousMonth, commissionRate, monthToDate.totalEds(), commission);
    }
}
