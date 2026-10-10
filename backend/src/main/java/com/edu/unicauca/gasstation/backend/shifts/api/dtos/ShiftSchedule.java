package com.edu.unicauca.gasstation.backend.shifts.api.dtos;

import java.util.List;

/**
 * Working segments of a shift. A segment whose endHour is lower than its startHour ends the next day.
 */
public record ShiftSchedule(List<Segment> segments, int totalHours) {

    public record Segment(int startHour, int endHour) {
    }
}
