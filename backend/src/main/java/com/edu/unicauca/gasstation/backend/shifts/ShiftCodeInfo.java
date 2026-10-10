package com.edu.unicauca.gasstation.backend.shifts;

import java.util.List;
import java.util.UUID;

/**
 * Read-only view of a shift code for other modules.
 * Lives in the module root so it is part of the public API of {@code shifts}.
 *
 * @param id              shift code identifier
 * @param code            code (DIA, DIA6, NOCHE, 12-7...)
 * @param name            display name
 * @param role            role of the code; null for DESCANSO
 * @param period          DAY or NIGHT; null for DESCANSO
 * @param segments        working segments; a segment whose end is lower than its start ends the next day
 * @param totalHours      sum of the hours of every segment
 * @param requiresSupport true for titular codes that need a support worker (DIA6, 9-6)
 * @param pairedWithId    only on support codes: the titular code it pairs with (12-7 -> DIA6)
 * @param active          whether the code can be used
 */
public record ShiftCodeInfo(
        UUID id,
        String code,
        String name,
        RoleInfo role,
        String period,
        List<Segment> segments,
        int totalHours,
        boolean requiresSupport,
        UUID pairedWithId,
        boolean active) {

    /** One block of working hours. */
    public record Segment(int startHour, int endHour) {
    }
}
