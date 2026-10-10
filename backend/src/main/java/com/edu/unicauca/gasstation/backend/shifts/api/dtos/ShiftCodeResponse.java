package com.edu.unicauca.gasstation.backend.shifts.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * Catalog entry exposed by the REST API. {@code role} and {@code period} are null for DESCANSO.
 *
 * @param id              shift code identifier
 * @param code            code (DIA, DIA6, NOCHE, 12-7...)
 * @param name            display name
 * @param role            role of the code
 * @param period          DAY or NIGHT
 * @param schedule        working segments and total hours
 * @param requiresSupport true for titular codes that need a support worker
 * @param pairedWithId    only on support codes: the titular code it pairs with
 * @param active          whether the code can be used
 */
@Schema(description = "Shift code of the catalog")
public record ShiftCodeResponse(
        @Schema(description = "Shift code identifier", example = "5ccc58dd-475c-46a7-a3be-8f246d9ebe99")
        UUID id,

        @Schema(description = "Shift code", example = "DIA6")
        String code,

        @Schema(description = "Display name", example = "Día 6 horas")
        String name,

        @Schema(description = "Role of the code; null for DESCANSO")
        RoleResponse role,

        @Schema(description = "DAY or NIGHT; null for DESCANSO", example = "DAY")
        String period,

        @Schema(description = "Working segments and total hours")
        ShiftSchedule schedule,

        @Schema(description = "True for titular codes that need a support worker (DIA6, 9-6)", example = "true")
        boolean requiresSupport,

        @Schema(description = "Only on support codes: id of the titular code it pairs with (12-7 pairs with DIA6)",
                example = "5ccc58dd-475c-46a7-a3be-8f246d9ebe99")
        UUID pairedWithId,

        @Schema(description = "Whether the code can be used", example = "true")
        boolean active) {
}
