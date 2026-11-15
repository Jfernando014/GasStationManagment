package com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.entities.ShiftAssignmentEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository of {@link ShiftAssignmentEntity}. Used only by {@code ShiftAssignmentRepositoryImpl}.
 * Queries load the shift code and its role in the same query ({@code @EntityGraph}) to avoid extra round trips.
 */
public interface JpaShiftAssignmentRepository extends JpaRepository<ShiftAssignmentEntity, UUID> {

    @EntityGraph(attributePaths = {"shiftCode", "shiftCode.role"})
    List<ShiftAssignmentEntity> findByWorkDateBetween(LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = {"shiftCode", "shiftCode.role"})
    Optional<ShiftAssignmentEntity> findByWorkerIdAndWorkDate(UUID workerId, LocalDate workDate);
}
