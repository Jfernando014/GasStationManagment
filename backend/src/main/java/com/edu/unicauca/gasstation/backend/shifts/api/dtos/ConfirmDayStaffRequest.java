package com.edu.unicauca.gasstation.backend.shifts.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.List;

/**
 * Optional body of {@code POST /api/shifts/day-staff/{date}/confirmation}.
 *
 * @param novelties novelties still pending, saved in the same transaction as the confirmation; may be omitted
 */
@Schema(description = "Novelties to save together with the confirmation. If marking step 1 fails, none is saved.")
public record ConfirmDayStaffRequest(
        @Schema(description = "Novelties not registered yet; omit or leave empty when they were registered before")
        List<@Valid RegisterNoveltyRequest> novelties) {
}
