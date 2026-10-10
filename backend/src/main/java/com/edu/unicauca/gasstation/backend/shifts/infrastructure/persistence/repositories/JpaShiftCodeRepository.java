package com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence.entities.ShiftCodeEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository of {@link ShiftCodeEntity}. Used only by {@code ShiftCodeRepositoryImpl}.
 * Both queries load the role in the same query ({@code @EntityGraph}) to avoid extra round trips.
 */
public interface JpaShiftCodeRepository extends JpaRepository<ShiftCodeEntity, UUID> {

    @EntityGraph(attributePaths = "role")
    Optional<ShiftCodeEntity> findByCode(String code);

    @EntityGraph(attributePaths = "role")
    List<ShiftCodeEntity> findByActiveTrue();
}
