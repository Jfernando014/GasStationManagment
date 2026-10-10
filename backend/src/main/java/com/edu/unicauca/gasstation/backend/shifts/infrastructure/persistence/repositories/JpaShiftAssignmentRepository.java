package com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.entities.ShiftAssignmentEntity;
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
 * Spring Data repository of {@link ShiftAssignmentEntity}. Used only by {@code ShiftAssignmentRepositoryImpl}.
 * Queries load the shift code and its role in the same query ({@code @EntityGraph}) to avoid extra round trips.
 */
public interface JpaShiftAssignmentRepository extends JpaRepository<ShiftAssignmentEntity, UUID> {

    @EntityGraph(attributePaths = {"shiftCode", "shiftCode.role"})
    List<ShiftAssignmentEntity> findByWorkDateBetween(LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = {"shiftCode", "shiftCode.role"})
    Optional<ShiftAssignmentEntity> findByWorkerIdAndWorkDate(UUID workerId, LocalDate workDate);

    /**
     * Deletes the assignments of a worker between both dates (inclusive) with a single DELETE, run immediately.
     * Not a derived {@code deleteBy...}: Hibernate would run the INSERTs of a regenerated range before those
     * DELETEs and break the unique (worker, date) constraint.
     *
     * @return number of deleted rows
     */
    @Modifying
    @Query("delete from ShiftAssignmentEntity a where a.workerId = :workerId and a.workDate between :from and :to")
    int deleteByWorkerInRange(@Param("workerId") UUID workerId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
