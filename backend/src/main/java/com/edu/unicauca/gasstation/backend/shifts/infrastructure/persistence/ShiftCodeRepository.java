package com.edu.unicauca.gasstation.backend.shifts.infrastructure.persistence;

import com.edu.unicauca.gasstation.backend.shifts.domain.ShiftCode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftCodeRepository extends JpaRepository<ShiftCode, UUID> {

    @EntityGraph(attributePaths = "role")
    Optional<ShiftCode> findByCode(String code);

    @EntityGraph(attributePaths = "role")
    List<ShiftCode> findByActiveTrue();
}
