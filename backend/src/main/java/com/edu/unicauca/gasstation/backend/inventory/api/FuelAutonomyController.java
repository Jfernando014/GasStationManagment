package com.edu.unicauca.gasstation.backend.inventory.api;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.FuelAutonomyResponse;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.FuelAutonomyService;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.FuelAutonomyMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/autonomy")
@RequiredArgsConstructor
@Tag(name = "Fuel autonomy", description = "Days of autonomy per fuel and alert level")
public class FuelAutonomyController {

    private final FuelAutonomyService autonomyService;
    private final FuelAutonomyMapper mapper;

    @GetMapping
    @Operation(summary = "Get the days of autonomy of each fuel on a journal date",
            description = "Measured stock divided by the average sales of the same day of the week. "
                    + "The level is WARNING or CRITICAL when the autonomy reaches the threshold in force.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "One row per fuel"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid journal date")
    })
    public List<FuelAutonomyResponse> findByJournalDate(
            @Parameter(description = "Journal date (yyyy-MM-dd)", example = "2026-08-05")
            @RequestParam LocalDate journalDate) {
        return mapper.toResponses(autonomyService.calculate(journalDate));
    }
}