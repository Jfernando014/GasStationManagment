package com.edu.unicauca.gasstation.backend.inventory.api;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.FuelPriceRequest;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.FuelPriceResponse;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelPrice;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.FuelPriceService;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.FuelPriceMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/inventory/fuel-prices")
@Tag(name = "Fuel prices", description = "Fuel prices with validity start dates")
public class FuelPriceController {

    private final FuelPriceService fuelPriceService;
    private final FuelPriceMapper fuelPriceMapper;

    public FuelPriceController(FuelPriceService fuelPriceService, FuelPriceMapper fuelPriceMapper) {
        this.fuelPriceService = fuelPriceService;
        this.fuelPriceMapper = fuelPriceMapper;
    }

    @Operation(summary = "Register a fuel price valid from a given date")
    @ApiResponse(responseCode = "201", description = "Price registered")
    @ApiResponse(responseCode = "400", description = "Invalid or missing fields")
    @ApiResponse(responseCode = "409", description = "The fuel already has a price starting on that date")
    @PostMapping
    public ResponseEntity<FuelPriceResponse> create(@Valid @RequestBody FuelPriceRequest request) {
        FuelPrice created = fuelPriceService.create(fuelPriceMapper.toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(fuelPriceMapper.toResponse(created));
    }

    @Operation(summary = "Price history, optionally filtered by fuel type")
    @ApiResponse(responseCode = "200", description = "Prices found")
    @GetMapping
    public List<FuelPriceResponse> findHistory(@RequestParam(required = false) FuelType fuelType) {
        return fuelPriceService.findHistory(fuelType).stream().map(fuelPriceMapper::toResponse).toList();
    }

    @Operation(summary = "Price in force for a fuel on a date (today if no date is given)")
    @ApiResponse(responseCode = "200", description = "Price found")
    @ApiResponse(responseCode = "404", description = "No price had started on that date")
    @GetMapping("/effective")
    public FuelPriceResponse findEffective(
            @RequestParam FuelType fuelType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        return fuelPriceMapper.toResponse(fuelPriceService.findEffective(fuelType, queryDate));
    }

    @Operation(summary = "Update a fuel price")
    @ApiResponse(responseCode = "200", description = "Price updated")
    @ApiResponse(responseCode = "400", description = "Invalid or missing fields")
    @ApiResponse(responseCode = "404", description = "Price not found")
    @ApiResponse(responseCode = "409", description = "The fuel already has a price starting on that date")
    @PutMapping("/{id}")
    public FuelPriceResponse update(@PathVariable Long id, @Valid @RequestBody FuelPriceRequest request) {
        return fuelPriceMapper.toResponse(fuelPriceService.update(id, fuelPriceMapper.toEntity(request)));
    }
}