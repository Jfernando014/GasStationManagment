package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.api.dto.DiscrepancyToleranceOverviewDto;
import com.edu.unicauca.gasstation.backend.sales.api.dto.DiscrepancyToleranceRequestDto;
import com.edu.unicauca.gasstation.backend.sales.api.dto.DiscrepancyToleranceResponseDto;
import com.edu.unicauca.gasstation.backend.sales.domain.models.DiscrepancyToleranceConfig;
import com.edu.unicauca.gasstation.backend.sales.domain.services.DiscrepancyToleranceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Discrepancy Tolerance Configuration", description = "Configuration of vendor discrepancy tolerance with effective dates")
@RestController
@RequestMapping("/api/v1/sales/configuration/tolerance")
@RequiredArgsConstructor
public class DiscrepancyToleranceController {

    private final DiscrepancyToleranceService service;

    @Operation(summary = "Get tolerance overview (active, scheduled and history)")
    @ApiResponse(responseCode = "200", description = "Overview returned successfully")
    @GetMapping
    public DiscrepancyToleranceOverviewDto getOverview(
            @Parameter(description = "Date for which the active tolerance is evaluated (defaults to today)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate evaluationDate = date != null ? date : LocalDate.now();
        DiscrepancyToleranceResponseDto active = DiscrepancyToleranceResponseDto.from(
                service.getEffectiveToleranceConfig(evaluationDate));

        List<DiscrepancyToleranceResponseDto> scheduled = service.getScheduledTolerances(evaluationDate).stream()
                .map(DiscrepancyToleranceResponseDto::from)
                .toList();

        List<DiscrepancyToleranceResponseDto> history = service.getToleranceHistory().stream()
                .map(DiscrepancyToleranceResponseDto::from)
                .toList();

        return new DiscrepancyToleranceOverviewDto(active, scheduled, history);
    }

    @Operation(summary = "Configure a new tolerance policy version with effective date")
    @ApiResponse(responseCode = "201", description = "Tolerance policy created")
    @ApiResponse(responseCode = "400", description = "Invalid tolerance data or closed journal conflict")
    @ApiResponse(responseCode = "403", description = "Admin authority required")
    @PostMapping
    public ResponseEntity<DiscrepancyToleranceResponseDto> configure(
            @Valid @RequestBody DiscrepancyToleranceRequestDto request,
            Authentication authentication) {
        SalesAdminAuthorization.requireAdmin(authentication);
        String username = authentication != null ? authentication.getName() : "admin";

        DiscrepancyToleranceConfig saved = service.configureTolerance(
                request.toleranceAmount(),
                request.effectiveDate(),
                request.notes(),
                username
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(DiscrepancyToleranceResponseDto.from(saved));
    }
}
