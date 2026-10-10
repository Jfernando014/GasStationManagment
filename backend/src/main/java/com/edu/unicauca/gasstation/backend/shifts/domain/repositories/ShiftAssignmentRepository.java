package com.edu.unicauca.gasstation.backend.shifts.domain.repositories;

import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftAssignment;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port for storing and reading the shift schedule. Implemented by
 * {@code infrastructure.persistence.adapters.ShiftAssignmentRepositoryImpl}. Assignments are always returned with
 * their shift code and its role loaded.
 */
public interface ShiftAssignmentRepository {

    /** Creates the assignment, or replaces its shift code and note when it already exists. */
    ShiftAssignment save(ShiftAssignment assignment);

    List<ShiftAssignment> saveAll(List<ShiftAssignment> assignments);

    /** Every assignment between both dates (inclusive). */
    List<ShiftAssignment> findByWorkDateBetween(LocalDate from, LocalDate to);

    Optional<ShiftAssignment> findByWorkerIdAndWorkDate(UUID workerId, LocalDate workDate);

    /**
     * Deletes the assignments of a worker between both dates (inclusive), immediately, so new assignments for
     * the same days can be saved right after without breaking the one-shift-per-day rule.
     *
     * @return number of deleted assignments
     */
    int deleteByWorkerInRange(UUID workerId, LocalDate from, LocalDate to);
}
