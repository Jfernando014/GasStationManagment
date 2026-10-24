package com.edu.unicauca.gasstation.backend.sales.domain.repositories;

import java.time.LocalDate;
import java.util.List;

/**
 * Secondary port for consuming assigned staff for an operational date (Turnos module).
 * Decouples sales domain from internal shifts entities while consuming dayStaffQuery.
 */
public interface DayStaffReadPort {

    List<AssignedVendor> findStaffForDate(LocalDate date);

    record AssignedVendor(
            String document,
            String name,
            String role,
            String shiftName,
            Integer pumpNumber,
            String workerCode
    ) {}
}
