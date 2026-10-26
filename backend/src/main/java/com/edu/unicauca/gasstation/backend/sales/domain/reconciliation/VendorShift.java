package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/** Hours of the day (0-23) that a vendor covers with his shift. */
public record VendorShift(UUID workerId, Set<Integer> hours) {

    public VendorShift {
        hours = Set.copyOf(hours);
    }

    /**
     * Expands shift segments to hours of the clock (0-23). A segment whose end is not after its start crosses
     * midnight (NOCHE 18-6 covers 18..23 and 0..5). The hours 0..5 belong to the operative day that began the
     * previous calendar day, see {@link RedebanTransaction#operativeDate()}.
     *
     * @param segments pairs {startHour, endHour}; endHour 24 means midnight
     */
    public static Set<Integer> hoursOf(List<int[]> segments) {
        Set<Integer> hours = new TreeSet<>();
        for (int[] segment : segments) {
            int start = segment[0];
            int end = segment[1] % 24;
            if (start == segment[1]) {
                continue;
            }
            for (int hour = start; hour != end; hour = (hour + 1) % 24) {
                hours.add(hour);
            }
        }
        return hours;
    }
}
