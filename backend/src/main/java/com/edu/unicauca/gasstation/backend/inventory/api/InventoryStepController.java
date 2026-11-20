package com.edu.unicauca.gasstation.backend.inventory.api;

import com.edu.unicauca.gasstation.backend.inventory.domain.services.InventoryStepResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/inventory/journal-step")
@RequiredArgsConstructor
@Tag(name = "Inventory journal step", description = "Step 5 of the journal: tanks and products")
public class InventoryStepController {

    private final InventoryStepResetService resetService;

    @DeleteMapping("/{journalDate}")
    @Operation(summary = "Reset step 5 of an open journal date",
            description = "Deletes the tank measurement and the entries, transfers and sales of the date, and returns "
                    + "step 5 to PENDING. The opening stock and the adjustments are kept.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Step 5 data cleared and the step returned to PENDING"),
            @ApiResponse(responseCode = "409", description = "Journal is closed and cannot be modified")
    })
    public ResponseEntity<Void> reset(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate journalDate) {
        resetService.reset(journalDate);
        return ResponseEntity.noContent().build();
    }
}
