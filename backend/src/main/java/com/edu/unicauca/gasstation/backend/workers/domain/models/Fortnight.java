package com.edu.unicauca.gasstation.backend.workers.domain.models;

import com.edu.unicauca.gasstation.backend.workers.exception.InvalidFortnightException;
import java.time.LocalDate;
import java.time.YearMonth;

/** A fortnight: the 1st (days 1 to 15) or the 2nd (day 16 to the last day of the month) of a month. */
public record Fortnight(YearMonth month, int number) {

    /** @throws InvalidFortnightException if the month is missing or the number is not 1 or 2 */
    public static Fortnight of(YearMonth month, int number) {
        if (month == null || number < 1 || number > 2) {
            throw new InvalidFortnightException("La quincena debe ser 1 (días 1 a 15) o 2 (día 16 al último del mes)");
        }
        return new Fortnight(month, number);
    }

    /** The fortnight that contains the date. */
    public static Fortnight containing(LocalDate date) {
        return new Fortnight(YearMonth.from(date), date.getDayOfMonth() <= 15 ? 1 : 2);
    }

    public LocalDate from() {
        return number == 1 ? month.atDay(1) : month.atDay(16);
    }

    public LocalDate to() {
        return number == 1 ? month.atDay(15) : month.atEndOfMonth();
    }
}
