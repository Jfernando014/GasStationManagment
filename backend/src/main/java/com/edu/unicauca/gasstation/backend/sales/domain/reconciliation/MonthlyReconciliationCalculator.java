package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Monthly table of the reconciliation (C-05). Pure Java: no Spring, no database.
 *
 * <ul>
 *   <li>One row per calendar day; days without data are zero rows, never missing.</li>
 *   <li>TOTAL EDS = Mastercard + Visa, as in the daily result.</li>
 *   <li>QR = Redeban when the day's file was imported, otherwise the planilla total.</li>
 *   <li>Commission = rate x TOTAL EDS of the month, HALF_UP to 2 decimals. QR never pays commission.</li>
 * </ul>
 *
 * <p>Values outside the month are ignored.
 */
public final class MonthlyReconciliationCalculator {

    private MonthlyReconciliationCalculator() {
    }

    /**
     * @param cardDays daily franchise totals; their {@code qr} is the sum of that day's planillas
     * @param redebanDays Redeban total of each day
     * @param pointsDays Puntos Colombia total of each day
     * @param commissionRate fraction effective on the last day of the month, e.g. 0.0265 for 2.65 %
     */
    public static MonthlyReconciliation calculate(YearMonth month, Map<LocalDate, DayFranchiseTotals> cardDays,
                                                  Map<LocalDate, BigDecimal> redebanDays,
                                                  Map<LocalDate, BigDecimal> pointsDays,
                                                  BigDecimal commissionRate) {
        Objects.requireNonNull(month, "month");
        Objects.requireNonNull(cardDays, "cardDays");
        Objects.requireNonNull(redebanDays, "redebanDays");
        Objects.requireNonNull(pointsDays, "pointsDays");
        if (commissionRate == null || commissionRate.signum() < 0 || commissionRate.compareTo(BigDecimal.ONE) > 0) {
            throw new ReconciliationValidationException("commissionRate", "La comisión debe estar entre 0 y 1");
        }
        List<MonthlyDayRow> rows = new ArrayList<>(month.lengthOfMonth());
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            rows.add(row(month.atDay(day), cardDays, redebanDays, pointsDays));
        }
        MonthlyTotals totals = totals(rows);
        BigDecimal amount = totals.totalEds().multiply(commissionRate).setScale(2, RoundingMode.HALF_UP);
        MonthlyCommission commission = new MonthlyCommission(commissionRate, month.atEndOfMonth(),
                totals.totalEds(), amount);
        int daysWithData = (int) rows.stream().filter(MonthlyDayRow::hasData).count();
        return new MonthlyReconciliation(month.toString(), month.atDay(1), month.atEndOfMonth(),
                List.copyOf(rows), daysWithData, totals, commission);
    }

    private static MonthlyDayRow row(LocalDate date, Map<LocalDate, DayFranchiseTotals> cardDays,
                                     Map<LocalDate, BigDecimal> redebanDays, Map<LocalDate, BigDecimal> pointsDays) {
        DayFranchiseTotals card = cardDays.getOrDefault(date, DayFranchiseTotals.ZERO);
        BigDecimal mastercard = scaled(card.mastercard());
        BigDecimal visa = scaled(card.visa());
        BigDecimal redeban = scaled(redebanDays.get(date));
        BigDecimal planillaQr = scaled(card.qr());
        MonthlyDayRow.QrSource source;
        BigDecimal qr;
        if (redeban.signum() > 0) {
            source = MonthlyDayRow.QrSource.REDEBAN;
            qr = redeban;
        } else if (planillaQr.signum() > 0) {
            source = MonthlyDayRow.QrSource.PLANILLA;
            qr = planillaQr;
        } else {
            source = MonthlyDayRow.QrSource.NONE;
            qr = Money.ZERO;
        }
        boolean hasData = cardDays.containsKey(date) || redebanDays.containsKey(date) || pointsDays.containsKey(date);
        BigDecimal totalEds = mastercard.add(visa);
        return new MonthlyDayRow(date, hasData, mastercard, visa, scaled(card.amex()), totalEds, qr, source,
                totalEds.add(qr), scaled(pointsDays.get(date)));
    }

    private static MonthlyTotals totals(List<MonthlyDayRow> rows) {
        BigDecimal mastercard = sum(rows, MonthlyDayRow::mastercard);
        BigDecimal visa = sum(rows, MonthlyDayRow::visa);
        BigDecimal qr = sum(rows, MonthlyDayRow::qr);
        BigDecimal totalEds = mastercard.add(visa);
        return new MonthlyTotals(mastercard, visa, sum(rows, MonthlyDayRow::amex), totalEds, qr,
                totalEds.add(qr), sum(rows, MonthlyDayRow::points));
    }

    private static BigDecimal sum(List<MonthlyDayRow> rows,
                                  java.util.function.Function<MonthlyDayRow, BigDecimal> field) {
        return rows.stream().map(field).reduce(Money.ZERO, BigDecimal::add);
    }

    /** Null is zero; the value keeps exactly 2 decimals (the database stores NUMERIC(14,2)). */
    private static BigDecimal scaled(BigDecimal value) {
        return value == null ? Money.ZERO : value.setScale(2, RoundingMode.UNNECESSARY);
    }
}
