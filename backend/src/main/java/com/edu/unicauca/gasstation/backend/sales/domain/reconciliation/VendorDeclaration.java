package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * What the administrator types for one vendor on one day: the datafono vouchers by franchise (HU-34) and the
 * figures declared in the vendor's planilla (Credibanco, QR, Puntos Colombia redemptions).
 *
 * <p>The "dawn" (madrugada) amounts are vouchers printed after midnight; the accounting report moves them to the
 * previous day, exactly as the Credibanco workbook does. Blank amounts are zero.
 */
public record VendorDeclaration(
        UUID workerId,
        String shiftCode,
        BigDecimal mastercard,
        BigDecimal visa,
        BigDecimal amex,
        BigDecimal mastercardDawn,
        BigDecimal visaDawn,
        BigDecimal amexDawn,
        BigDecimal planillaCredibanco,
        BigDecimal planillaQr,
        BigDecimal planillaRedemptions) {

    public VendorDeclaration {
        Objects.requireNonNull(workerId, "workerId");
        shiftCode = shiftCode == null || shiftCode.isBlank() ? null : shiftCode.strip();
        mastercard = Money.amount("mastercard", mastercard);
        visa = Money.amount("visa", visa);
        amex = Money.amount("amex", amex);
        mastercardDawn = Money.amount("mastercardDawn", mastercardDawn);
        visaDawn = Money.amount("visaDawn", visaDawn);
        amexDawn = Money.amount("amexDawn", amexDawn);
        planillaCredibanco = Money.amount("planillaCredibanco", planillaCredibanco);
        planillaQr = Money.amount("planillaQr", planillaQr);
        planillaRedemptions = Money.amount("planillaRedemptions", planillaRedemptions);
    }

    /** Mastercard + Visa + Amex, dawn included: what is compared with the planilla's Credibanco. */
    public BigDecimal vouchersTotal() {
        return mastercard.add(visa).add(amex).add(mastercardDawn).add(visaDawn).add(amexDawn);
    }
}
