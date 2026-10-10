package com.edu.unicauca.gasstation.backend.shifts.domain.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/**
 * Shift code a worker has on a given day. Mapped to the {@code shift_assignment} table (migration V8).
 *
 * <p>A worker has a single assignment per day; changing a day means replacing its shift code. DESCANSO is stored
 * as an assignment too, so a day without assignment is "not scheduled".
 *
 * <p>{@code workerId} is a plain UUID, without {@code @ManyToOne}: workers belong to the {@code workers} module.
 * The foreign key exists only in the database (V8).
 */
@Entity
@Table(name = "shift_assignment", uniqueConstraints = @UniqueConstraint(
        name = "uk_shift_assignment_worker_date", columnNames = {"worker_id", "work_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShiftAssignment {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "worker_id", nullable = false)
    private UUID workerId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_code_id", nullable = false)
    private ShiftCode shiftCode;

    /** Optional reason for the change; null when there is none. */
    @Column(length = 255)
    private String note;

    public ShiftAssignment(UUID workerId, LocalDate workDate, ShiftCode shiftCode, String note) {
        this.workerId = Objects.requireNonNull(workerId, "workerId");
        this.workDate = Objects.requireNonNull(workDate, "workDate");
        change(shiftCode, note);
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
