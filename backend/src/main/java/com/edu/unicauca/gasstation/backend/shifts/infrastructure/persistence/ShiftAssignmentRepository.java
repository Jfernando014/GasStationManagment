package com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence;

import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftAssignment;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data repository of {@link ShiftAssignment}.
 */
public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignment, UUID> {

    /** Every assignment between both dates (inclusive), with its shift code and role loaded in the same query. */
    @EntityGraph(attributePaths = {"shiftCode", "shiftCode.role"})
    List<ShiftAssignment> findByWorkDateBetween(LocalDate from, LocalDate to);

    Optional<ShiftAssignment> findByWorkerIdAndWorkDate(UUID workerId, LocalDate workDate);

    /**
     * Deletes the assignments of a worker between both dates (inclusive) with a single DELETE, run immediately.
     * Not a derived {@code deleteBy...}: Hibernate would run the INSERTs of a regenerated range before those
     * DELETEs and break the unique (worker, date) constraint.
     *
     * @return number of deleted rows
     */
    @Modifying
    @Query("delete from ShiftAssignment a where a.workerId = :workerId and a.workDate between :from and :to")
    int deleteByWorkerInRange(@Param("workerId") UUID workerId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
