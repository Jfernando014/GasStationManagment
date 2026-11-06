package com.edu.unicauca.gasstation.backend.inventory.api;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.InventoryHomeResponse;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.InventoryHomeService;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.InventoryHomeMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory/home")
@RequiredArgsConstructor
@Tag(name = "Inventory home", description = "Current stock of the fuels and alerts, for the home screen")
public class InventoryHomeController {

    private final InventoryHomeService homeService;
    private final InventoryHomeMapper mapper;

    @GetMapping
    @Operation(summary = "Get the inventory data of the home screen",
            description = "Stock per fuel and per tank from the last measurement of each tank, "
                    + "the most critical pending alert and the number of pending alerts.")
    @ApiResponse(responseCode = "200", description = "Home data")
    public InventoryHomeResponse get() {
        return mapper.toResponse(homeService.get());
    }
}