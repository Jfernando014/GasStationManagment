package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * One row of the Redeban CSV. {@code date}, {@code time}, {@code type} and {@code reference} are null when the
 * file has no such column (or the cell is empty). {@code approved} is null when the file has no status column.
 * {@code row} is the 1-based line number in the file.
 */
public record RedebanTransaction(LocalDate date, LocalTime time, BigDecimal amount, String type, String reference,
                                 int row, Boolean approved) {

    /** Row of a file without status information: it is taken as approved. */
    public RedebanTransaction(LocalDate date, LocalTime time, BigDecimal amount, String type, String reference,
                              int row) {
        this(date, time, amount, type, reference, row, null);
    }

    /** HU-42 #1: only approved transactions are added. Unknown status counts as approved. */
    public boolean isApproved() {
        return approved == null || approved;
    }

    /** The operative day starts at 06:00 and ends at 06:00 of the next calendar day. */
    public static final int OPERATIVE_DAY_START_HOUR = 6;

    /**
     * Operative day of the row. The operative day runs from 06:00 to 06:00, so a transaction between 00:00 and
     * 05:59 belongs to the day that started the previous calendar day (the night of day X belongs to day X).
     * Null when the row has no date; a row with a date but no time keeps its calendar date.
     */
    public LocalDate operativeDate() {
        if (date == null) {
            return null;
        }
        return time != null && time.getHour() < OPERATIVE_DAY_START_HOUR ? date.minusDays(1) : date;
    }

    /** Without a type column every row is a QR payment; with one, only the rows whose type mentions QR. */
    public boolean isQr() {
        return type == null || type.toLowerCase(java.util.Locale.ROOT).contains("qr");
    }
}
