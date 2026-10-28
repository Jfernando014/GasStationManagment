package com.edu.unicauca.gasstation.backend.inventory.api;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.AutonomyThresholdRequest;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.AutonomyThresholdResponse;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.AutonomyThresholdService;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.AutonomyThresholdMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/autonomy-thresholds")
@RequiredArgsConstructor
@Tag(name = "Autonomy thresholds", description = "Warning and critical days of autonomy, with validity date")
public class AutonomyThresholdController {

    private final AutonomyThresholdService thresholdService;
    private final AutonomyThresholdMapper mapper;

    @PostMapping
    @Operation(summary = "Create the thresholds that apply from a date on",
            description = "Existing thresholds are never modified. The critical threshold must be lower than the warning.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Thresholds created"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "409", description = "Thresholds already exist for that validity date")
    })
    public ResponseEntity<AutonomyThresholdResponse> create(@Valid @RequestBody AutonomyThresholdRequest request) {
        AutonomyThresholdResponse response =
                mapper.toResponse(thresholdService.create(mapper.toDomain(request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List all the thresholds, most recent validity first")
    @ApiResponse(responseCode = "200", description = "Thresholds (empty list if none is configured)")
    public List<AutonomyThresholdResponse> findAll() {
        return mapper.toResponses(thresholdService.findAll());
    }
}