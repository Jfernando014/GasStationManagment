package com.edu.unicauca.gasstation.backend.sales;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Verified attendance only, never scheduled assignments. An available snapshot certifies all
 * actually worked seller shifts, including shifts with no sales, for the inclusive operational-date range.
 * Non-selling staff are excluded; one unique ID represents one worked shift, not an attendance event.
 * Operational dates must follow the same night-shift attribution as BasketSalesReadPort.
 */
public interface WorkedShiftsReadPort {
    Snapshot read(LocalDate fromInclusive, LocalDate toInclusive);

    record Snapshot(boolean available, List<WorkedShift> shifts) {
        public Snapshot {
            shifts = List.copyOf(shifts);
            if (!available && !shifts.isEmpty()) throw new IllegalArgumentException("Unavailable source cannot supply attendance");
        }
    }

    record WorkedShift(String id, UUID workerId, LocalDate date) {
        public WorkedShift {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("Worked shift ID required");
            Objects.requireNonNull(workerId, "workerId");
            Objects.requireNonNull(date, "date");
            if (workerId.equals(new UUID(0, 0))) throw new IllegalArgumentException("Valid worker ID required");
        }
    }
}
