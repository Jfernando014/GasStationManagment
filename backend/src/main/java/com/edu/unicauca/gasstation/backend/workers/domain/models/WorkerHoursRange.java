package com.edu.unicauca.gasstation.backend.workers.domain.models;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Hours of one worker over a range of dates: the stored days in date order and their total. {@code pendingDates}
 * are the dates of the range whose processing failed and are therefore missing from the total.
 */
public record WorkerHoursRange(UUID workerId, String workerName, LocalDate from, LocalDate to, int totalHours,
                               List<WorkerDayHours> days, List<LocalDate> pendingDates) {
}
