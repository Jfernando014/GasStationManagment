package com.edu.unicauca.gasstation.backend.shifts;

import com.edu.unicauca.gasstation.backend.shifts.api.dtos.ShiftCodeResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Only entry point other modules may use to query the shift catalog.
 */
public interface ShiftExternalService {

    Optional<UUID> findShiftCodeIdByCode(String code);

    List<ShiftCodeResponse> getActiveCatalog();
}
