package com.edu.unicauca.gasstation.backend.shifts.domain.models;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * Shift code a worker has on a given day.
 *
 * <p>A worker has a single assignment per day; changing a day means replacing its shift code. DESCANSO is stored
 * as an assignment too, so a day without assignment is "not scheduled".
 *
 * <p>{@code workerId} is a plain UUID: workers belong to the {@code workers} module.
 */
@Getter
public class ShiftAssignment {

    /** Null until the assignment is saved. */
    private final UUID id;

    private final UUID workerId;

    private final LocalDate workDate;

    private ShiftCode shiftCode;

    /** Optional reason for the change; null when there is none. */
    private String note;

    /**
     * Creates a new assignment.
     */
    public ShiftAssignment(UUID workerId, LocalDate workDate, ShiftCode shiftCode, String note) {
        this(null, workerId, workDate, shiftCode, note);
    }

    private ShiftAssignment(UUID id, UUID workerId, LocalDate workDate, ShiftCode shiftCode, String note) {
        this.id = id;
        this.workerId = Objects.requireNonNull(workerId, "workerId");
        this.workDate = Objects.requireNonNull(workDate, "workDate");
        change(shiftCode, note);
    }

    /**
     * Rebuilds an assignment that already exists (for example, read from the database).
     */
    public static ShiftAssignment restore(UUID id, UUID workerId, LocalDate workDate, ShiftCode shiftCode, String note) {
        return new ShiftAssignment(Objects.requireNonNull(id, "id"), workerId, workDate, shiftCode, note);
    }

    /**
     * Replaces the shift code and the note of the day. The note is kept without leading or trailing spaces,
     * and a blank note is stored as null.
     */
    public void change(ShiftCode shiftCode, String note) {
        this.shiftCode = Objects.requireNonNull(shiftCode, "shiftCode");
        this.note = note == null || note.isBlank() ? null : note.strip();
    }
}
