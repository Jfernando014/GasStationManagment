package com.edu.unicauca.gasstation.backend.sales.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Balance rules over a list of dated movements. Pure logic: no Spring, no database. */
public final class FundLedger {

    private FundLedger() {
    }

    /**
     * Walks the ledger date by date (movements of the same day are netted) and returns the
     * first date, on or after {@code from}, at which the running balance is negative.
     * Movements before {@code from} still count toward the running balance.
     */
    public static Optional<NegativeBalance> firstNegativeFrom(List<LedgerEntry> entries, LocalDate from) {
        List<LedgerEntry> sorted = entries.stream()
                .sorted(Comparator.comparing(LedgerEntry::date))
                .toList();

        BigDecimal running = BigDecimal.ZERO;
        for (LedgerEntry entry : sorted) {
            running = running.add(entry.signedAmount());
            if (!entry.date().isBefore(from) && running.compareTo(BigDecimal.ZERO) < 0) {
                return Optional.of(new NegativeBalance(entry.date(), running));
            }
        }
        return Optional.empty();
    }
}
