package com.edu.unicauca.gasstation.backend.shifts.api;

import com.edu.unicauca.gasstation.backend.shifts.api.dtos.ShiftCodeResponse;
import com.edu.unicauca.gasstation.backend.shifts.domain.services.ShiftCatalogService;
import com.edu.unicauca.gasstation.backend.shifts.infrastructure.mappers.ShiftCodeMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only endpoints of the shift catalog. Delegates to {@link ShiftCatalogService} and converts the
 * domain models with {@link ShiftCodeMapper}.
 */
@Tag(name = "Shift catalog", description = "Read-only catalog of the 11 shift codes with their schedule, hours, "
        + "role and pairing rule between support and titular codes.")
@RestController
@RequestMapping("/api/shifts/catalog")
@RequiredArgsConstructor
public class ShiftCodeController {

    private final ShiftCatalogService shiftCatalogService;
    private final ShiftCodeMapper shiftCodeMapper;

    @Operation(summary = "List active shift codes",
            description = "Each code includes its role (null for DESCANSO), its segments and total hours. "
                    + "A segment whose end hour is lower than its start hour ends the next day (NOCHE).")
    @GetMapping
    public List<ShiftCodeResponse> listActive() {
        return shiftCatalogService.listActive().stream().map(shiftCodeMapper::toResponse).toList();
    }

    @Operation(summary = "Get a shift code by its code",
            description = "Returns the code whether it is active or not.")
    @ApiResponse(responseCode = "404", description = "Shift code not found",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{code}")
    public ShiftCodeResponse getByCode(
            @Parameter(description = "Shift code, for example DIA6 or 12-7", example = "DIA6")
            @PathVariable String code) {
        return shiftCodeMapper.toResponse(shiftCatalogService.getByCode(code));
    }
}
