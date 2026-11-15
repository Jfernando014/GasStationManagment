package com.edu.unicauca.gasstation.backend.workers.domain.models;

/** One block of worked hours. A segment whose end is lower than its start ends the next day. */
public record DayHoursSegment(int startHour, int endHour, int hours) {

    /** A segment with its hours worked out from its start and end. */
    public static DayHoursSegment of(int startHour, int endHour) {
        int hours = endHour > startHour ? endHour - startHour : endHour + 24 - startHour;
        return new DayHoursSegment(startHour, endHour, hours);
    }
}
