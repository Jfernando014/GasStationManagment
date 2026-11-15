package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One calendar day of the monthly reconciliation table (C-05).
 *
 * @param hasData the day has vendor rows, a Redeban import or a Puntos Colombia total
 * @param totalEds Mastercard + Visa: no Amex, no QR and no dawn vouchers
 * @param qrSource where {@code qr} came from; {@code NONE} when the day has no QR at all
 * @param totalWithQr {@code totalEds + qr}
 * @param points Puntos Colombia total of the day; zero when none was loaded
 */
public record MonthlyDayRow(
        LocalDate date,
        boolean hasData,
        BigDecimal mastercard,
        BigDecimal visa,
        BigDecimal amex,
        BigDecimal totalEds,
        BigDecimal qr,
        QrSource qrSource,
        BigDecimal totalWithQr,
        BigDecimal points) {

    /** Origin of the QR amount of a day. */
    public enum QrSource {
        REDEBAN,
        PLANILLA,
        NONE
    }
}
