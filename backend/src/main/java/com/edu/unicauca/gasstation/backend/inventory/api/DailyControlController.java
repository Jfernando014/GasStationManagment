package com.edu.unicauca.gasstation.backend.inventory.api;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.DailyControlResponse;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.DailyControlService;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.DailyControlMapper;
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

@RestController
@RequestMapping("/api/v1/inventory/daily-control")
@RequiredArgsConstructor
@Tag(name = "Daily inventory control", description = "Variation, autonomy and product stock of a journal")
public class DailyControlController {

    private final DailyControlService controlService;
    private final DailyControlMapper mapper;

    @GetMapping
    @Operation(summary = "Get the inventory control of a journal date",
            description = "Per fuel: variation, days of autonomy and estimated depletion date. "
                    + "Per product: units sold and closing stock in the office and the counter.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Control of the journal"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid journal date")
    })
    public DailyControlResponse findByJournalDate(
            @Parameter(description = "Journal date (yyyy-MM-dd)", example = "2026-08-05")
            @RequestParam LocalDate journalDate) {
        return mapper.toResponse(controlService.findByJournalDate(journalDate));
    }
}