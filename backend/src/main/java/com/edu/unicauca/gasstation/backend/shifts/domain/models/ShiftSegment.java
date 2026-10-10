package com.edu.unicauca.gasstation.backend.shifts.domain.models;

/**
 * One block of working hours of a shift code.
 *
 * @param startHour hour the block starts (0-23)
 * @param endHour   hour the block ends (1-24); lower than {@code startHour} means it ends the next day
 */
public record ShiftSegment(int startHour, int endHour) {
}
