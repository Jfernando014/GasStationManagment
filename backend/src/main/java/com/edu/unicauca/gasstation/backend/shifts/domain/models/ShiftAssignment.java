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
 * <p>Every assignment records who put it ({@link ShiftOrigin}): the automatic rotation rewrites only generated days,
 * so a change made by hand survives a new rotation. A generated day never has a note.
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

    private ShiftOrigin origin;

    /**
     * Creates a new assignment set by the administrator ({@link ShiftOrigin#MANUAL}).
     */
    public ShiftAssignment(UUID workerId, LocalDate workDate, ShiftCode shiftCode, String note) {
        this(null, workerId, workDate, shiftCode, note, ShiftOrigin.MANUAL);
    }

    private ShiftAssignment(UUID id, UUID workerId, LocalDate workDate, ShiftCode shiftCode, String note,
                            ShiftOrigin origin) {
        this.id = id;
        this.workerId = Objects.requireNonNull(workerId, "workerId");
        this.workDate = Objects.requireNonNull(workDate, "workDate");
        this.shiftCode = Objects.requireNonNull(shiftCode, "shiftCode");
        this.note = normalizeNote(note);
        this.origin = Objects.requireNonNull(origin, "origin");
    }

    /**
     * Creates a new assignment written by the automatic rotation: origin {@link ShiftOrigin#GENERATED} and no note.
     */
    public static ShiftAssignment generated(UUID workerId, LocalDate workDate, ShiftCode shiftCode) {
        return new ShiftAssignment(null, workerId, workDate, shiftCode, null, ShiftOrigin.GENERATED);
    }

    /**
     * Rebuilds an assignment that already exists (for example, read from the database), with its origin.
     */
    public static ShiftAssignment restore(UUID id, UUID workerId, LocalDate workDate, ShiftCode shiftCode, String note,
                                          ShiftOrigin origin) {
        return new ShiftAssignment(Objects.requireNonNull(id, "id"), workerId, workDate, shiftCode, note, origin);
    }

    /**
     * Replaces the shift code and the note of the day by hand: the day becomes {@link ShiftOrigin#MANUAL}. The note
     * is kept without leading or trailing spaces, and a blank note is stored as null.
     */
    public void change(ShiftCode shiftCode, String note) {
        this.shiftCode = Objects.requireNonNull(shiftCode, "shiftCode");
        this.note = normalizeNote(note);
        this.origin = ShiftOrigin.MANUAL;
    }

    /**
     * Rewrites the day from the automatic rotation: it becomes {@link ShiftOrigin#GENERATED} and loses its note.
     */
    public void regenerate(ShiftCode shiftCode) {
        this.shiftCode = Objects.requireNonNull(shiftCode, "shiftCode");
        this.note = null;
        this.origin = ShiftOrigin.GENERATED;
    }

    private static String normalizeNote(String note) {
        return note == null || note.isBlank() ? null : note.strip();
    }
}
