package com.edu.unicauca.gasstation.backend.shifts.api.dtos;

import com.edu.unicauca.gasstation.backend.shifts.NoveltyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Body of {@code POST /api/shifts/day-staff/{date}/novelties}, and each pending novelty of the confirmation.
 *
 * @param type                REPLACEMENT, ABSENCE, SHIFT_CHANGE or SWAP
 * @param workerId            absent worker, worker whose code changes or first worker of the swap
 * @param counterpartWorkerId replacement (REPLACEMENT) or second worker (SWAP); omitted otherwise
 * @param shiftCode           code to work (SHIFT_CHANGE, optional in REPLACEMENT)
 * @param exceptionalCoverage accepts a code of another role
 * @param reason              3 to 255 characters
 */
@Schema(description = "Novelty of the day staff. The grid is not modified: the original and the final code are kept.")
public record RegisterNoveltyRequest(
        @Schema(description = "REPLACEMENT (workerId is absent and counterpartWorkerId covers), ABSENCE (nobody "
                + "covers), SHIFT_CHANGE (workerId works shiftCode; also adds a resting worker) or SWAP (workerId and "
                + "counterpartWorkerId swap their codes)", example = "REPLACEMENT")
        @NotNull(message = "El tipo de novedad es obligatorio")
        NoveltyType type,

        @Schema(description = "Absent worker (REPLACEMENT, ABSENCE), worker whose code changes (SHIFT_CHANGE) or "
                + "first worker of the swap", example = "d3c8c22b-9a86-4678-a326-ac90b26057cb")
        @NotNull(message = "El vendedor es obligatorio")
        UUID workerId,

        @Schema(description = "Replacement (REPLACEMENT) or second worker of the swap (SWAP); omit it otherwise",
                example = "0f8c1b8e-31a3-4c42-9a51-3e2f63a4f5b1")
        UUID counterpartWorkerId,

        @Schema(description = "Code to work. Required in SHIFT_CHANGE; in REPLACEMENT the code of the absent worker "
                + "is used when omitted; ignored in ABSENCE and SWAP", example = "NOCHE")
        String shiftCode,

        @Schema(description = "Accepts a code of a role different from the worker's role", example = "false")
        boolean exceptionalCoverage,

        @Schema(description = "Reason, 3 to 255 characters without leading and trailing spaces",
                example = "Incapacidad médica")
        String reason) {
}
