package com.edu.unicauca.gasstation.backend.sales.domain.services;

import com.edu.unicauca.gasstation.backend.sales.domain.models.CashDifferenceStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * The cash balance of step 4 (C-06) as the "Saldo en caja" sheet of the Excel computes it. The only place that
 * defines these formulas.
 *
 * <pre>
 * planilla balance = cash movements balance + station cash of every day from the opening day up to the date
 * difference       = Dominus accumulated balance - planilla balance
 * </pre>
 *
 * <p>The cash movements balance is {@code initial balance + adjustments - deposits}. The station cash of a day is
 * the sum of the cash the sellers declared in step 2 ({@code sellerCloseQuery}). Example, 01/08/2026:
 * 12.114.000 + 12.516.000 = 24.630.000 in the planilla against 24.629.469 in Dominus: difference -531.
 *
 * <p>A negative difference means that the planilla has more cash than Dominus.
 */
public final class CashBalanceCalculator {

    private static final int MONEY_SCALE = 2;

    private CashBalanceCalculator() {
    }

    /** Planilla balance: the cash movements balance plus the station cash accumulated since the opening day. */
    public static BigDecimal planillaBalance(BigDecimal cashMovementsBalance, BigDecimal accumulatedStationCash) {
        Objects.requireNonNull(cashMovementsBalance, "cashMovementsBalance");
        Objects.requireNonNull(accumulatedStationCash, "accumulatedStationCash");
        return cashMovementsBalance.add(accumulatedStationCash).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** Dominus accumulated balance minus planilla balance. */
    public static BigDecimal difference(BigDecimal dominusAccumulatedBalance, BigDecimal planillaBalance) {
        Objects.requireNonNull(dominusAccumulatedBalance, "dominusAccumulatedBalance");
        Objects.requireNonNull(planillaBalance, "planillaBalance");
        return dominusAccumulatedBalance.subtract(planillaBalance).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public static CashDifferenceStatus statusOf(BigDecimal difference) {
        Objects.requireNonNull(difference, "difference");
        int sign = difference.signum();
        if (sign == 0) {
            return CashDifferenceStatus.CUADRA;
        }
        return sign < 0 ? CashDifferenceStatus.PLANILLA_MAYOR : CashDifferenceStatus.PLANILLA_MENOR;
    }
}
