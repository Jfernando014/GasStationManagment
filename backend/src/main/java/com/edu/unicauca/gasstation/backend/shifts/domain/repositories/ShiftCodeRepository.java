package com.edu.unicauca.gasstation.backend.shifts.domain.repositories;

import com.edu.unicauca.gasstation.backend.shifts.domain.models.ShiftCode;
import java.util.List;
import java.util.Optional;

/**
 * Port for storing and reading shift codes. Implemented by
 * {@code infrastructure.persistence.adapters.ShiftCodeRepositoryImpl}. Shift codes are always returned with
 * their role loaded.
 */
public interface ShiftCodeRepository {

    ShiftCode save(ShiftCode shiftCode);

    Optional<ShiftCode> findByCode(String code);

    List<ShiftCode> findActive();
}
