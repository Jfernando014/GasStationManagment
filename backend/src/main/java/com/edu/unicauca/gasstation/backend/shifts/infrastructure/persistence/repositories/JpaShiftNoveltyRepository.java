package com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.entities.ShiftNoveltyEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository of {@link ShiftNoveltyEntity}. Used only by {@code ShiftNoveltyRepositoryImpl}.
 * Queries load both shift codes and their roles in the same query ({@code @EntityGraph}).
 */
public interface JpaShiftNoveltyRepository extends JpaRepository<ShiftNoveltyEntity, UUID> {

    @EntityGraph(attributePaths = {"originalShiftCode", "originalShiftCode.role", "finalShiftCode", "finalShiftCode.role"})
    List<ShiftNoveltyEntity> findByWorkDateBetweenAndCancelledAtIsNullOrderByCreatedAtAscIdAsc(LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = {"originalShiftCode", "originalShiftCode.role", "finalShiftCode", "finalShiftCode.role"})
    Optional<ShiftNoveltyEntity> findWithCodesById(UUID id);
}
