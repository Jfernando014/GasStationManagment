package com.edu.unicauca.gasstation.backend.shifts.domain.services;

import com.edu.unicauca.gasstation.backend.shifts.holidays.HolidayQuery;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Implements the public {@link HolidayQuery} on top of {@link HolidayService}, which it does not change. */
@Service
@RequiredArgsConstructor
public class HolidayQueryService implements HolidayQuery {

    private final HolidayService holidayService;

    @Override
    public boolean isHoliday(LocalDate date) {
        Objects.requireNonNull(date, "date");
        return holidayService.isHoliday(date);
    }

    @Override
    public Set<LocalDate> getHolidays(LocalDate from, LocalDate to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la fecha inicial");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("El rango no puede superar " + MAX_RANGE_DAYS + " días");
        }
        Set<LocalDate> holidays = new TreeSet<>();
        for (YearMonth month = YearMonth.from(from); !month.isAfter(YearMonth.from(to)); month = month.plusMonths(1)) {
            holidayService.getHolidays(month).stream()
                    .filter(date -> !date.isBefore(from) && !date.isAfter(to))
                    .forEach(holidays::add);
        }
        return holidays;
    }
}
