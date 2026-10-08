package com.edu.unicauca.gasstation.backend.shifts.api.dtos;

import java.util.UUID;

/**
 * Catalog entry exposed to clients and other modules. {@code role} and {@code period} are null for DESCANSO.
 * {@code period} is DAY or NIGHT, sent as text so other modules do not depend on the internal enum.
 */
public record ShiftCodeResponse(
        UUID id,
        String code,
        String name,
        RoleResponse role,
        String period,
        ShiftSchedule schedule,
        boolean requiresSupport,
        UUID pairedWithId,
        boolean active) {
}
