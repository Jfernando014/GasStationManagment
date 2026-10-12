package com.edu.unicauca.gasstation.backend.inventory.api;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.GaugeTableRequest;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.GaugeTableResponse;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.GaugeVolumeResponse;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.GaugeTable;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.GaugeTableService;
import com.edu.unicauca.gasstation.backend.inventory.exception.InvalidGaugeTableException;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.GaugeTableMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@Validated
@Tag(name = "Gauge tables", description = "Calibration tables (height to gallons) shared by tanks, and volume calculation")
public class GaugeTableController {

    private final GaugeTableService gaugeTableService;
    private final GaugeTableMapper gaugeTableMapper;

    public GaugeTableController(GaugeTableService gaugeTableService, GaugeTableMapper gaugeTableMapper) {
        this.gaugeTableService = gaugeTableService;
        this.gaugeTableMapper = gaugeTableMapper;
    }

    @Operation(summary = "Create a gauge table",
            description = "Success message for the UI: 'Tabla de aforo guardada correctamente'")
    @ApiResponse(responseCode = "201", description = "Gauge table created")
    @ApiResponse(responseCode = "400", description = "Invalid points or missing fields")
    @ApiResponse(responseCode = "409", description = "A gauge table with the same name already exists")
    @PostMapping("/gauge-tables")
    public ResponseEntity<GaugeTableResponse> create(@Valid @RequestBody GaugeTableRequest request) {
        GaugeTable created = gaugeTableService.create(gaugeTableMapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @Operation(summary = "Create a gauge table from a CSV file",
            description = "Columns: height in cm and volume in gallons, optional header. Separator ',' (dots as "
                    + "decimals), ';' or tab (commas allowed as decimals). Rows must already be in order.")
    @ApiResponse(responseCode = "201", description = "Gauge table created")
    @ApiResponse(responseCode = "400", description = "Invalid file or points")
    @ApiResponse(responseCode = "409", description = "A gauge table with the same name already exists")
    @PostMapping(value = "/gauge-tables/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GaugeTableResponse> createFromCsv(
            @Parameter(description = "Name of the new gauge table") @RequestParam("name") String name,
            @Parameter(description = "CSV file (UTF-8)") @RequestParam("file") MultipartFile file) {
        GaugeTable created = gaugeTableService.createFromCsv(name, readCsv(file));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @Operation(summary = "List gauge tables ordered by name")
    @ApiResponse(responseCode = "200", description = "Gauge tables found")
    @GetMapping("/gauge-tables")
    public List<GaugeTableResponse> findAll() {
        return gaugeTableService.findAll().stream().map(this::toResponse).toList();
    }

    @Operation(summary = "Get a gauge table by id")
    @ApiResponse(responseCode = "200", description = "Gauge table found")
    @ApiResponse(responseCode = "404", description = "Gauge table not found")
    @GetMapping("/gauge-tables/{id}")
    public GaugeTableResponse findById(@PathVariable Long id) {
        return toResponse(gaugeTableService.findById(id));
    }

    @Operation(summary = "Replace the name and all the points of a gauge table",
            description = "Success message for the UI: 'Tabla de aforo actualizada correctamente'. "
                    + "The change applies to every tank that uses the table.")
    @ApiResponse(responseCode = "200", description = "Gauge table updated")
    @ApiResponse(responseCode = "400", description = "Invalid points or missing fields")
    @ApiResponse(responseCode = "404", description = "Gauge table not found")
    @ApiResponse(responseCode = "409", description = "Another gauge table already uses the name")
    @PutMapping("/gauge-tables/{id}")
    public GaugeTableResponse update(@PathVariable Long id, @Valid @RequestBody GaugeTableRequest request) {
        return toResponse(gaugeTableService.update(id, gaugeTableMapper.toDomain(request)));
    }

    @Operation(summary = "Replace the points of a gauge table from a CSV file",
            description = "Keeps the table name. Success message for the UI: 'Tabla de aforo actualizada correctamente'")
    @ApiResponse(responseCode = "200", description = "Gauge table updated")
    @ApiResponse(responseCode = "400", description = "Invalid file or points")
    @ApiResponse(responseCode = "404", description = "Gauge table not found")
    @PutMapping(value = "/gauge-tables/{id}/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GaugeTableResponse replaceFromCsv(
            @PathVariable Long id,
            @Parameter(description = "CSV file (UTF-8)") @RequestParam("file") MultipartFile file) {
        return toResponse(gaugeTableService.replacePointsFromCsv(id, readCsv(file)));
    }

    @Operation(summary = "Download a gauge table as CSV",
            description = "Columns Altura_cm,Volumen_Galones; the file can be imported again as is")
    @ApiResponse(responseCode = "200", description = "CSV file")
    @ApiResponse(responseCode = "404", description = "Gauge table not found")
    @GetMapping(value = "/gauge-tables/{id}/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(@PathVariable Long id) {
        GaugeTable table = gaugeTableService.findById(id);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Tabla Aforo.csv\"")
                .body(gaugeTableService.toCsv(table).getBytes(StandardCharsets.UTF_8));
    }

    @Operation(summary = "Calculate the gallons of a tank from the height measured with the ruler",
            description = "Interpolates the tank's gauge table and subtracts the configured dead volume "
                    + "(never below 0)")
    @ApiResponse(responseCode = "200", description = "Gallons calculated")
    @ApiResponse(responseCode = "400", description = "Missing or negative height, or height above the table maximum")
    @ApiResponse(responseCode = "404", description = "Tank not found")
    @ApiResponse(responseCode = "409", description = "The tank has no gauge table configured")
    @GetMapping("/tanks/{tankId}/volume")
    public GaugeVolumeResponse calculateVolume(
            @PathVariable Long tankId,
            @Parameter(description = "Height in cm, e.g. 55 or 55.5")
            @RequestParam @DecimalMin(value = "0", message = "The height cannot be negative") BigDecimal heightCm) {
        return new GaugeVolumeResponse(tankId, heightCm, gaugeTableService.calculateVolume(tankId, heightCm));
    }

    private GaugeTableResponse toResponse(GaugeTable table) {
        return gaugeTableMapper.toResponse(table, gaugeTableService.findTanksUsing(table.getId()));
    }

    private String readCsv(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new InvalidGaugeTableException("file", "Was not able to read the file");
        }
    }
}