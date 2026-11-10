package com.edu.unicauca.gasstation.backend.inventory.api;

import com.edu.unicauca.gasstation.backend.inventory.InspectionCategory;
import com.edu.unicauca.gasstation.backend.inventory.InspectionStatus;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.InspectionRequest;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.InspectionResponse;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Inspection;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.InspectionService;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.InspectionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/inspections")
@RequiredArgsConstructor
@Validated
@Tag(name = "Inspections", description = "Inspection, hermeticity and ONAC calibration log of the station")
public class InspectionController {

    private final InspectionService inspectionService;
    private final InspectionMapper mapper;

    @PostMapping
    @Operation(summary = "Register an inspection",
            description = "The due date must be after the execution date and the code must be unique.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Inspection registered"),
            @ApiResponse(responseCode = "400", description = "Invalid or missing fields"),
            @ApiResponse(responseCode = "409", description = "Another inspection already uses the code")
    })
    public ResponseEntity<InspectionResponse> create(@Valid @RequestBody InspectionRequest request) {
        Inspection created = inspectionService.create(mapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit an inspection")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inspection updated"),
            @ApiResponse(responseCode = "400", description = "Invalid or missing fields"),
            @ApiResponse(responseCode = "404", description = "Inspection not found"),
            @ApiResponse(responseCode = "409", description = "Another inspection already uses the code")
    })
    public ResponseEntity<InspectionResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody InspectionRequest request) {
        Inspection updated = inspectionService.update(id, mapper.toDomain(request));
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an inspection")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inspection found"),
            @ApiResponse(responseCode = "404", description = "Inspection not found")
    })
    public ResponseEntity<InspectionResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(inspectionService.findById(id)));
    }

    @GetMapping
    @Operation(summary = "Inspection log",
            description = "Filters by category, regulatory status and execution date range (all optional). "
                    + "Nearest due date first. The status is computed with the server date.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Log (possibly empty)"),
            @ApiResponse(responseCode = "400", description = "Invalid filter or the start date is after the end date")
    })
    public ResponseEntity<List<InspectionResponse>> search(
            @RequestParam(required = false) InspectionCategory category,
            @RequestParam(required = false) InspectionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(mapper.toResponses(inspectionService.search(category, status, from, to)));
    }
}
