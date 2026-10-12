package com.edu.unicauca.gasstation.backend.shifts.domain.services;

import static org.assertj.core.api.Assertions.assertThat;

import de.focus_shift.jollyday.core.Holiday;
import de.focus_shift.jollyday.core.HolidayCalendar;
import de.focus_shift.jollyday.core.HolidayManager;
import de.focus_shift.jollyday.core.ManagerParameters;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

/**
 * Dates checked against the official 2026 calendar of Colombia (Easter Sunday is April 5, 2026).
 */
class HolidayServiceTest {

    private static final String CHIQUINQUIRA_KEY = "DAY_OF_OUR_LADY_OF_THE_ROSARY_OF_CHIQUINQUIRA";

    private final HolidayService holidayService = new HolidayService();

    @Test
    void fixedMovedAndEasterBasedHolidaysAreHolidays() {
        assertThat(holidayService.isHoliday(LocalDate.of(2026, 7, 20))).as("fixed").isTrue();
        assertThat(holidayService.isHoliday(LocalDate.of(2026, 12, 25))).as("fixed").isTrue();
        assertThat(holidayService.isHoliday(LocalDate.of(2026, 1, 12))).as("Epiphany moved to Monday").isTrue();
        assertThat(holidayService.isHoliday(LocalDate.of(2026, 4, 3))).as("Good Friday").isTrue();
    }

    @Test
    void ordinaryDaysAndTheOriginalDateOfAMovedHolidayAreNotHolidays() {
        assertThat(holidayService.isHoliday(LocalDate.of(2026, 3, 10))).isFalse();
        assertThat(holidayService.isHoliday(LocalDate.of(2026, 1, 6))).as("Tuesday, moved to Monday 12").isFalse();
    }

    @Test
    void getHolidaysReturnsTheDatesOfTheMonthInOrder() {
        // Maundy Thursday, Good Friday and Easter Sunday (kept on purpose by the service)
        assertThat(holidayService.getHolidays(YearMonth.of(2026, 4))).containsExactly(
                LocalDate.of(2026, 4, 2), LocalDate.of(2026, 4, 3), LocalDate.of(2026, 4, 5));
        assertThat(holidayService.getHolidays(YearMonth.of(2026, 9))).isEmpty();
    }

    @Test
    void chiquinquiraHolidayIsNotCounted() {
        LocalDate chiquinquira = jollydayDateOfChiquinquira(2026);

        assertThat(chiquinquira).as("July 9, 2026 is a Thursday, Jollyday moves it to Monday")
                .isEqualTo(LocalDate.of(2026, 7, 13));
        assertThat(holidayService.isHoliday(chiquinquira)).isFalse();
        assertThat(holidayService.getHolidays(YearMonth.from(chiquinquira))).containsExactly(LocalDate.of(2026, 7, 20));
    }

    /** The date Jollyday itself gives to the Chiquinquirá holiday in {@code year}. */
    private static LocalDate jollydayDateOfChiquinquira(int year) {
        HolidayManager manager = HolidayManager.getInstance(ManagerParameters.create(HolidayCalendar.COLOMBIA));
        return manager.getHolidays(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)).stream()
                .filter(holiday -> CHIQUINQUIRA_KEY.equals(holiday.getPropertiesKey()))
                .map(Holiday::getDate)
                .findFirst()
                .orElseThrow();
    }
}
