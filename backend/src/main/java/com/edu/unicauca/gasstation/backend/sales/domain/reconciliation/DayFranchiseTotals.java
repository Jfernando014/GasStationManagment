package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

import java.math.BigDecimal;
import java.util.List;

/** Franchise totals of one day. TOTAL EDS is Mastercard + Visa: Amex and QR are not part of it (HU-34 #3). */
public record DayFranchiseTotals(
        BigDecimal amex,
        BigDecimal mastercard,
        BigDecimal visa,
        BigDecimal amexDawn,
        BigDecimal mastercardDawn,
        BigDecimal visaDawn,
        BigDecimal qr) {

    public static final DayFranchiseTotals ZERO = new DayFranchiseTotals(
            Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO);

    public BigDecimal totalEds() {
        return mastercard.add(visa);
    }

    public DayFranchiseTotals plus(DayFranchiseTotals other) {
        return new DayFranchiseTotals(amex.add(other.amex), mastercard.add(other.mastercard), visa.add(other.visa),
                amexDawn.add(other.amexDawn), mastercardDawn.add(other.mastercardDawn),
                visaDawn.add(other.visaDawn), qr.add(other.qr));
    }

    public static DayFranchiseTotals of(List<VendorDeclaration> declarations, BigDecimal qr) {
        DayFranchiseTotals total = ZERO;
        for (VendorDeclaration d : declarations) {
            total = total.plus(new DayFranchiseTotals(d.amex(), d.mastercard(), d.visa(),
                    d.amexDawn(), d.mastercardDawn(), d.visaDawn(), Money.ZERO));
        }
        return new DayFranchiseTotals(total.amex, total.mastercard, total.visa, total.amexDawn,
                total.mastercardDawn, total.visaDawn, Money.amount("qr", qr));
    }
}
