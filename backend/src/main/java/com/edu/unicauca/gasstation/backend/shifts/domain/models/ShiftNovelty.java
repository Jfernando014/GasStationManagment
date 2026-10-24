package com.edu.unicauca.gasstation.backend.shifts.domain.models;

import com.edu.unicauca.gasstation.backend.shifts.NoveltyType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

/**
 * Change of last minute to the staff of a journal day (S2-19, step 1). The grid is never modified: the novelty keeps
 * the original code of the grid and the code actually worked (RF-D8).
 *
 * <p>Who is who, by type:
 * <ul>
 *   <li>REPLACEMENT: {@code workerId} is absent and {@code counterpartWorkerId} works {@code finalShiftCode}.</li>
 *   <li>ABSENCE: {@code workerId} is absent and nobody covers; there is no final code.</li>
 *   <li>SHIFT_CHANGE: {@code workerId} works {@code finalShiftCode} instead of the grid code.</li>
 *   <li>SWAP: {@code workerId} works {@code finalShiftCode} (the code of the counterpart) and
 *       {@code counterpartWorkerId} works {@code originalShiftCode}.</li>
 * </ul>
 * {@code originalShiftCode} is always the grid code of {@code workerId} (DESCANSO when the worker had none).
 * A cancelled novelty is kept for traceability and is no longer applied (CB-D4).
 */
@Getter
public class ShiftNovelty {

    /** Null until the novelty is saved. */
    private final UUID id;
    private final LocalDate workDate;
    private final NoveltyType type;
    private final UUID workerId;
    /** Null for ABSENCE and SHIFT_CHANGE. */
    private final UUID counterpartWorkerId;
    private final ShiftCode originalShiftCode;
    /** Null for ABSENCE. */
    private final ShiftCode finalShiftCode;
    /** A code of another role was accepted on purpose (VAL-D5). */
    private final boolean exceptionalCoverage;
    /** Without leading or trailing spaces. */
    private final String reason;
    private final Instant createdAt;
    /** Null while the novelty applies. */
    private Instant cancelledAt;

    private ShiftNovelty(UUID id, LocalDate workDate, NoveltyType type, UUID workerId, UUID counterpartWorkerId,
                         ShiftCode originalShiftCode, ShiftCode finalShiftCode, boolean exceptionalCoverage,
                         String reason, Instant createdAt, Instant cancelledAt) {
        if (type == null || type == NoveltyType.NONE) {
            throw new IllegalArgumentException("A novelty needs a real type, not " + type);
        }
        boolean needsCounterpart = type == NoveltyType.REPLACEMENT || type == NoveltyType.SWAP;
        if (needsCounterpart != (counterpartWorkerId != null) || Objects.equals(workerId, counterpartWorkerId)) {
            throw new IllegalArgumentException("Invalid counterpart for a " + type + " novelty");
        }
        if ((type == NoveltyType.ABSENCE) != (finalShiftCode == null)) {
            throw new IllegalArgumentException("Invalid final code for a " + type + " novelty");
        }
        this.id = id;
        this.workDate = Objects.requireNonNull(workDate, "workDate");
        this.type = type;
        this.workerId = Objects.requireNonNull(workerId, "workerId");
        this.counterpartWorkerId = counterpartWorkerId;
        this.originalShiftCode = Objects.requireNonNull(originalShiftCode, "originalShiftCode");
        this.finalShiftCode = finalShiftCode;
        this.exceptionalCoverage = exceptionalCoverage;
        this.reason = Objects.requireNonNull(reason, "reason").strip();
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.cancelledAt = cancelledAt;
    }

    /** A new novelty, not saved yet. */
    public static ShiftNovelty register(LocalDate workDate, NoveltyType type, UUID workerId, UUID counterpartWorkerId,
                                        ShiftCode originalShiftCode, ShiftCode finalShiftCode,
                                        boolean exceptionalCoverage, String reason, Instant createdAt) {
        return new ShiftNovelty(null, workDate, type, workerId, counterpartWorkerId, originalShiftCode,
                finalShiftCode, exceptionalCoverage, reason, createdAt, null);
    }

    /** Rebuilds a novelty that already exists (for example, read from the database). */
    public static ShiftNovelty restore(UUID id, LocalDate workDate, NoveltyType type, UUID workerId,
                                       UUID counterpartWorkerId, ShiftCode originalShiftCode,
                                       ShiftCode finalShiftCode, boolean exceptionalCoverage, String reason,
                                       Instant createdAt, Instant cancelledAt) {
        return new ShiftNovelty(Objects.requireNonNull(id, "id"), workDate, type, workerId, counterpartWorkerId,
                originalShiftCode, finalShiftCode, exceptionalCoverage, reason, createdAt, cancelledAt);
    }

    /** Stops applying the novelty; the day goes back to the grid code. Cancelling twice keeps the first time. */
    public void cancel(Instant when) {
        if (cancelledAt == null) {
            cancelledAt = Objects.requireNonNull(when, "when");
        }
    }

    public boolean isActive() {
        return cancelledAt == null;
    }

    /** True when the worker is the affected worker or the counterpart. */
    public boolean involves(UUID worker) {
        return workerId.equals(worker) || worker.equals(counterpartWorkerId);
    }
}
