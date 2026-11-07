package com.edu.unicauca.gasstation.backend.inventory.api;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.AlertCountResponse;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.AttendAlertRequest;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.InventoryAlertResponse;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.AlertStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.InventoryAlertService;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.InventoryAlertMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/alerts")
@RequiredArgsConstructor
@Tag(name = "Inventory alerts", description = "Alerts raised when the days of autonomy of a fuel reach the thresholds")
public class InventoryAlertController {

    private final InventoryAlertService alertService;
    private final InventoryAlertMapper mapper;

    @GetMapping
    @Operation(summary = "List the alerts",
            description = "Without a filter it returns the open ones (active, read and attended), critical first. "
                    + "Use the status filter to see the resolved ones.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alerts (empty list if there are none)"),
            @ApiResponse(responseCode = "400", description = "Invalid status")
    })
    public List<InventoryAlertResponse> findAll(
            @Parameter(description = "Optional filter: ACTIVE, READ, ATTENDED or RESOLVED")
            @RequestParam(required = false) AlertStatus status) {
        return mapper.toResponses(alertService.findAll(status));
    }

    @GetMapping("/count")
    @Operation(summary = "Count the alerts nobody has attended yet",
            description = "Active and read alerts: the number shown on the bell.")
    @ApiResponse(responseCode = "200", description = "Number of pending alerts")
    public AlertCountResponse countPending() {
        return new AlertCountResponse(alertService.countPending());
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark an alert as read",
            description = "Idempotent: an alert that is not active is returned unchanged.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The alert"),
            @ApiResponse(responseCode = "404", description = "Alert not found")
    })
    public InventoryAlertResponse markRead(@PathVariable Long id) {
        return mapper.toResponse(alertService.markRead(id));
    }

    @PutMapping("/{id}/attend")
    @Operation(summary = "Mark an alert as attended, with an optional note",
            description = "The alert stays in the tray while the condition lasts, but it no longer counts on the bell.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The alert"),
            @ApiResponse(responseCode = "400", description = "Invalid note"),
            @ApiResponse(responseCode = "404", description = "Alert not found"),
            @ApiResponse(responseCode = "409", description = "The alert is already resolved")
    })
    public InventoryAlertResponse attend(@PathVariable Long id, @Valid @RequestBody AttendAlertRequest request) {
        return mapper.toResponse(alertService.attend(id, request.getNote()));
    }
}