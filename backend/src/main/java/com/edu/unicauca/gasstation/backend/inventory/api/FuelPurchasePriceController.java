package com.edu.unicauca.gasstation.backend.inventory.api;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.FuelPurchasePriceRequest;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.FuelPurchasePriceResponse;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelPurchasePrice;
import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.FuelPurchasePriceService;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.FuelPurchasePriceMapper;
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
@RequestMapping("/api/v1/inventory/fuel-purchase-prices")
@Tag(name = "Fuel purchase prices", description = "Fuel purchase costs per gallon with validity start dates")
public class FuelPurchasePriceController {

    private final FuelPurchasePriceService fuelPurchasePriceService;
    private final FuelPurchasePriceMapper fuelPurchasePriceMapper;

    public FuelPurchasePriceController(FuelPurchasePriceService fuelPurchasePriceService,
                                       FuelPurchasePriceMapper fuelPurchasePriceMapper) {
        this.fuelPurchasePriceService = fuelPurchasePriceService;
        this.fuelPurchasePriceMapper = fuelPurchasePriceMapper;
    }

    @Operation(summary = "Register a fuel purchase price valid from a given date")
    @ApiResponse(responseCode = "201", description = "Price registered")
    @ApiResponse(responseCode = "400", description = "Invalid or missing fields")
    @ApiResponse(responseCode = "409", description = "The fuel already has a price starting on that date")
    @PostMapping
    public ResponseEntity<FuelPurchasePriceResponse> create(@Valid @RequestBody FuelPurchasePriceRequest request) {
        FuelPurchasePrice created = fuelPurchasePriceService.create(fuelPurchasePriceMapper.toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(fuelPurchasePriceMapper.toResponse(created));
    }

    @Operation(summary = "Purchase price history, optionally filtered by fuel type")
    @ApiResponse(responseCode = "200", description = "Prices found")
    @GetMapping
    public List<FuelPurchasePriceResponse> findHistory(@RequestParam(required = false) FuelType fuelType) {
        return fuelPurchasePriceService.findHistory(fuelType).stream()
                .map(fuelPurchasePriceMapper::toResponse)
                .toList();
    }

    @Operation(summary = "Purchase price in force for a fuel on a date (today if no date is given)")
    @ApiResponse(responseCode = "200", description = "Price found")
    @ApiResponse(responseCode = "404", description = "No price had started on that date")
    @GetMapping("/effective")
    public FuelPurchasePriceResponse findEffective(
            @RequestParam FuelType fuelType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        return fuelPurchasePriceMapper.toResponse(fuelPurchasePriceService.findEffective(fuelType, queryDate));
    }

    @Operation(summary = "Update a fuel purchase price")
    @ApiResponse(responseCode = "200", description = "Price updated")
    @ApiResponse(responseCode = "400", description = "Invalid or missing fields")
    @ApiResponse(responseCode = "404", description = "Price not found")
    @ApiResponse(responseCode = "409", description = "The fuel already has a price starting on that date")
    @PutMapping("/{id}")
    public FuelPurchasePriceResponse update(@PathVariable Long id,
                                            @Valid @RequestBody FuelPurchasePriceRequest request) {
        return fuelPurchasePriceMapper.toResponse(
                fuelPurchasePriceService.update(id, fuelPurchasePriceMapper.toEntity(request)));
    }
}
