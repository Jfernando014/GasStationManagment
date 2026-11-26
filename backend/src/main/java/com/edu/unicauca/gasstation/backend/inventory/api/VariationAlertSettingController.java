package com.edu.unicauca.gasstation.backend.inventory.api;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.VariationAlertSettingRequest;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.VariationAlertSettingResponse;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.VariationAlertSettingService;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.VariationAlertSettingMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/variation-alert-settings")
@RequiredArgsConstructor
@Tag(name = "Variation alert settings", description = "Rules of the monthly variation alerts, with validity date")
public class VariationAlertSettingController {

    private final VariationAlertSettingService settingService;
    private final VariationAlertSettingMapper mapper;

    @PostMapping
    @Operation(summary = "Create the alert rules that apply from a date on",
            description = "A range of larger shortages cannot allow more occurrences than a range of smaller ones.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Setting created"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "409", description = "A setting already exists for that validity date")
    })
    public ResponseEntity<VariationAlertSettingResponse> create(
            @Valid @RequestBody VariationAlertSettingRequest request) {
        VariationAlertSettingResponse response =
                mapper.toResponse(settingService.create(mapper.toDomain(request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update existing alert rules",
            description = "The validity date must remain unique. The oldest one cannot change its date.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Setting updated"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "404", description = "Setting not found"),
            @ApiResponse(responseCode = "409",
                    description = "Validity date already used, or the date of the oldest setting was changed")
    })
    public VariationAlertSettingResponse update(
            @PathVariable Long id,
            @Valid @RequestBody VariationAlertSettingRequest request) {
        return mapper.toResponse(settingService.update(id, mapper.toDomain(request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an alert setting", description = "The oldest one cannot be deleted.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Setting deleted"),
            @ApiResponse(responseCode = "404", description = "Setting not found"),
            @ApiResponse(responseCode = "409", description = "It is the oldest setting")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        settingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "List all the alert settings, most recent validity first")
    @ApiResponse(responseCode = "200", description = "Settings")
    public List<VariationAlertSettingResponse> findAll() {
        return mapper.toResponses(settingService.findAll());
    }
}
