package com.edu.unicauca.gasstation.backend.workers.domain.models;

import java.util.List;

/** The staff of a day as the shifts module reports it, already turned into records to keep. */
public record DayHoursReading(boolean noveltiesApplied, List<WorkerDayHours> records) {

    public DayHoursReading {
        records = List.copyOf(records);
    }
}
