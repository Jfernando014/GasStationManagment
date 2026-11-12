package com.edu.unicauca.gasstation.backend.workers.domain.models;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * One day of a fortnight sheet. {@code holiday} is true for a Sunday or a public holiday. The hours are those of the
 * shift that started on this day, even if it ends the next morning, classified with the parameters in force on this
 * day. {@code workedCode} is null on a rest day or a day without record.
 *
 * @param grossHours          hours kept for the day, with no rest taken off
 * @param parametersValidFrom start of the parameter version used for the day
 */
public record OvertimeDay(LocalDate date, DayOfWeek dayOfWeek, boolean holiday, String workedCode, int grossHours,
                          CategoryHours hours, LocalDate parametersValidFrom) {
}
