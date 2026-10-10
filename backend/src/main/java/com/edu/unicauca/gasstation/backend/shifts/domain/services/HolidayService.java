package com.edu.unicauca.gasstation.backend.shifts.domain.services;

import de.focus_shift.jollyday.core.Holiday;
import de.focus_shift.jollyday.core.HolidayCalendar;
import de.focus_shift.jollyday.core.HolidayManager;
import de.focus_shift.jollyday.core.ManagerParameters;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

/**
 * Tells whether a date is a public holiday in Colombia. Holidays are calculated with Jollyday; nothing is
 * stored in the database.
 *
 * <p>Dates are the day off actually observed: holidays moved to Monday (Ley Emiliani) are returned on that
 * Monday. Ordinary Sundays are not treated as holidays here; that rule does not belong to this service.
 * Easter Sunday is returned because Jollyday lists it for Colombia, and it is kept on purpose.
 */
@Service
public class HolidayService {

    /**
     * Excluded on purpose: Jollyday includes this holiday (July 9, moved to Monday) from 2026, but the law that
     * created it is in dispute, so the station does not treat it as a holiday.
     */
    private static final String DAY_OF_OUR_LADY_OF_THE_ROSARY_OF_CHIQUINQUIRA =
            "DAY_OF_OUR_LADY_OF_THE_ROSARY_OF_CHIQUINQUIRA";

    private final HolidayManager holidayManager;

    public HolidayService() {
        this.holidayManager = HolidayManager.getInstance(ManagerParameters.create(HolidayCalendar.COLOMBIA));
    }

    /** True if {@code date} is a holiday in Colombia, except the Chiquinquirá holiday. */
    public boolean isHoliday(LocalDate date) {
        return holidaysBetween(date, date).anyMatch(holiday -> holiday.getDate().equals(date));
    }

    /** Holidays of {@code month} in Colombia, except the Chiquinquirá holiday, in ascending order. */
    public Set<LocalDate> getHolidays(YearMonth month) {
        return holidaysBetween(month.atDay(1), month.atEndOfMonth())
                .map(Holiday::getDate)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Holidays between both dates (inclusive), without the excluded one. {@link Holiday#getDate()} is the observed
     * day off (already moved to Monday when it applies).
     */
    private Stream<Holiday> holidaysBetween(LocalDate from, LocalDate to) {
        return holidayManager.getHolidays(from, to).stream()
                .filter(holiday -> !DAY_OF_OUR_LADY_OF_THE_ROSARY_OF_CHIQUINQUIRA.equals(holiday.getPropertiesKey()));
    }
}
