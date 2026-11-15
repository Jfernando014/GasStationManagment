package com.edu.unicauca.gasstation.backend.workers.infrastructure.shifts;

import com.edu.unicauca.gasstation.backend.shifts.holidays.HolidayQuery;
import com.edu.unicauca.gasstation.backend.workers.domain.ports.HolidayCalendarPort;
import java.time.LocalDate;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Reads the public holidays from the query that shifts publishes. */
@Component
@RequiredArgsConstructor
public class ShiftsHolidayAdapter implements HolidayCalendarPort {

    private final HolidayQuery holidayQuery;

    @Override
    public Set<LocalDate> holidaysBetween(LocalDate from, LocalDate to) {
        return holidayQuery.getHolidays(from, to);
    }
}
