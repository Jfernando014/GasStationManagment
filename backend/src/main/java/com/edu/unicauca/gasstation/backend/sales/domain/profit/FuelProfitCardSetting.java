package com.edu.unicauca.gasstation.backend.sales.domain.profit;

import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.ReconciliationValidationException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Objects;

/**
 * Card percentage setting of a month.
 *
 * @param month yyyy-MM
 * @param manualPercentage from 0 to 100; present only in MANUAL mode
 */
public record FuelProfitCardSetting(String month, CardPercentageMode mode, BigDecimal manualPercentage) {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public FuelProfitCardSetting {
        Objects.requireNonNull(month, "month");
        Objects.requireNonNull(mode, "mode");
        if (mode == CardPercentageMode.REAL) {
            manualPercentage = null;
        } else if (manualPercentage == null) {
            throw new ReconciliationValidationException("manualPercentage",
                    "El porcentaje manual es obligatorio en modo MANUAL");
        } else if (manualPercentage.signum() < 0 || manualPercentage.compareTo(HUNDRED) > 0) {
            throw new ReconciliationValidationException("manualPercentage",
                    "El porcentaje manual debe estar entre 0 y 100");
        }
        manualPercentage = Decimals.plain(manualPercentage);
    }

    /** The setting of a month that has none registered. */
    public static FuelProfitCardSetting real(YearMonth month) {
        return new FuelProfitCardSetting(month.toString(), CardPercentageMode.REAL, null);
    }
}
