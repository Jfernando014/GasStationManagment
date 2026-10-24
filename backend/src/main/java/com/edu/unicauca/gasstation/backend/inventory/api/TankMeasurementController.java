package com.edu.unicauca.gasstation.backend.inventory.api;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.TankMeasurementRequest;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.TankMeasurementResponse;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.TankMeasurement;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.TankMeasurementService;
import com.edu.unicauca.gasstation.backend.inventory.exception.TankMeasurementNotFoundException;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.TankMeasurementMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Validated
@Tag(name = "Tank measurements", description = "Ruler measurement of the tanks of a journal")
public class TankMeasurementController {

    private final TankMeasurementService measurementService;
    private final TankMeasurementMapper mapper;

    @PutMapping("/measurements/{journalDate}")
    @Operation(summary = "Save the measurement of a journal",
            description = "Calculates the gallons of each reading with the gauge table of its tank. "
                    + "If the journal already has a measurement, the readings sent replace those of the same tank.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Measurement saved"),
            @ApiResponse(responseCode = "400", description = "Invalid height, repeated tank or height above the gauge table"),
            @ApiResponse(responseCode = "404", description = "Tank not found"),
            @ApiResponse(responseCode = "409", description = "A tank has no gauge table")
    })
    public ResponseEntity<TankMeasurementResponse> save(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate journalDate,
            @Valid @RequestBody TankMeasurementRequest request) {
        TankMeasurement saved = measurementService.save(journalDate, mapper.toReadings(request.getReadings()));
        return ResponseEntity.ok(mapper.toResponse(saved));
    }

    @GetMapping("/measurements/{journalDate}")
    @Operation(summary = "Get the measurement of a journal")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Measurement found"),
            @ApiResponse(responseCode = "404", description = "The journal has no measurement")
    })
    public ResponseEntity<TankMeasurementResponse> findByJournalDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate journalDate) {
        TankMeasurement found = measurementService.findByJournalDate(journalDate)
                .orElseThrow(() -> TankMeasurementNotFoundException.forDate(journalDate));
        return ResponseEntity.ok(mapper.toResponse(found));
    }

    @GetMapping("/measurements/previous")
    @Operation(summary = "Get the last measurement before a journal",
            description = "It is the opening stock of the journal: the most recent measurement of an earlier date.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Measurement found"),
            @ApiResponse(responseCode = "404", description = "There is no earlier measurement")
    })
    public ResponseEntity<TankMeasurementResponse> findPrevious(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate journalDate) {
        TankMeasurement found = measurementService.findLatestBefore(journalDate)
                .orElseThrow(() -> TankMeasurementNotFoundException.before(journalDate));
        return ResponseEntity.ok(mapper.toResponse(found));
    }
}