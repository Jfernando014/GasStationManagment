package com.edu.unicauca.gasstation.backend.inventory.api;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.FuelReceptionRequest;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.FuelReceptionResponse;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelReception;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.FuelReceptionService;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.FuelReceptionMapper;
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
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Validated
@Tag(name = "Fuel receptions", description = "Fuel receptions (consignments) of each tank and their history")
public class FuelReceptionController {

    private final FuelReceptionService receptionService;
    private final FuelReceptionMapper mapper;

    @PostMapping("/receptions")
    @Operation(summary = "Register a fuel reception",
            description = "The reception must fit in the tank: last measured gallons plus the receptions "
                    + "of later journals plus this quantity cannot exceed the tank capacity.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reception registered"),
            @ApiResponse(responseCode = "400", description = "Invalid quantity, supplier or date"),
            @ApiResponse(responseCode = "404", description = "Tank not found"),
            @ApiResponse(responseCode = "409", description = "The quantity exceeds the available capacity of the tank")
    })
    public ResponseEntity<FuelReceptionResponse> create(@Valid @RequestBody FuelReceptionRequest request) {
        FuelReception created = receptionService.create(mapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(created));
    }

    @PutMapping("/receptions/{id}")
    @Operation(summary = "Edit a fuel reception",
            description = "Replaces the data of the reception and validates the tank capacity again.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reception updated"),
            @ApiResponse(responseCode = "400", description = "Invalid quantity, supplier or date"),
            @ApiResponse(responseCode = "404", description = "Reception or tank not found"),
            @ApiResponse(responseCode = "409", description = "The quantity exceeds the available capacity of the tank")
    })
    public ResponseEntity<FuelReceptionResponse> update(@PathVariable Long id,
                                                        @Valid @RequestBody FuelReceptionRequest request) {
        FuelReception updated = receptionService.update(id, mapper.toDomain(request));
        return ResponseEntity.ok(mapper.toResponse(updated));
    }

    @GetMapping("/receptions/{id}")
    @Operation(summary = "Get a fuel reception")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reception found"),
            @ApiResponse(responseCode = "404", description = "Reception not found")
    })
    public ResponseEntity<FuelReceptionResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(mapper.toResponse(receptionService.findById(id)));
    }

    @GetMapping("/receptions")
    @Operation(summary = "Reception history",
            description = "Filters by tank and/or date range (all optional). Most recent first. "
                    + "An empty list means there are no receptions for the filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "History (possibly empty)"),
            @ApiResponse(responseCode = "400", description = "The start date is after the end date")
    })
    public ResponseEntity<List<FuelReceptionResponse>> search(
            @RequestParam(required = false) Long tankId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(mapper.toResponses(receptionService.search(tankId, from, to)));
    }
}