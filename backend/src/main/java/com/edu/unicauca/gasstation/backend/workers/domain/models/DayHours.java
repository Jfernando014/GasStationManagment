package com.edu.unicauca.gasstation.backend.workers.domain.models;

import java.util.List;

/** The processing state of a date and the hours stored for each worker of that day. */
public record DayHours(HoursSync sync, List<WorkerDayHours> workers) {
}
