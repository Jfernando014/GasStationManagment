package com.edu.unicauca.gasstation.backend.inventory.api;

import com.edu.unicauca.gasstation.backend.inventory.api.dto.TankRequest;
import com.edu.unicauca.gasstation.backend.inventory.api.dto.TankResponse;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Tank;
import com.edu.unicauca.gasstation.backend.inventory.domain.services.TankService;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.mappers.TankMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/tanks")
public class TankController {
    private final TankService tankService;
    private final TankMapper tankMapper;

    public TankController(TankService tankService, TankMapper tankMapper) {
        this.tankService = tankService;
        this.tankMapper = tankMapper;
    }

    @Operation(summary = "Register a tank")
    @ApiResponse(responseCode = "201", description = "Tank registered")
    @ApiResponse(responseCode = "400", description = "Invalid or missing fields")
    @ApiResponse(responseCode = "409", description = "A tank with the same code already exists")
    @PostMapping
    public ResponseEntity<TankResponse> create(@Valid @RequestBody TankRequest request) {
        Tank created = tankService.createTank(tankMapper.toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(tankMapper.toResponse(created));
    }

    @Operation(summary = "List all tanks ordered by code")
    @ApiResponse(responseCode = "200", description = "Tanks found")
    @GetMapping
    public List<TankResponse> findAll() {
        return tankService.findAllTanks().stream().map(tankMapper::toResponse).toList();
    }

    @Operation(summary = "Get a tank by id")
    @ApiResponse(responseCode = "200", description = "Tank found")
    @ApiResponse(responseCode = "404", description = "Tank not found")
    @GetMapping("/{id}")
    public TankResponse findById(@PathVariable Long id) {
        return tankMapper.toResponse(tankService.findTankById(id));
    }

    @Operation(summary = "Update a tank")
    @ApiResponse(responseCode = "200", description = "Tank updated")
    @ApiResponse(responseCode = "400", description = "Invalid or missing fields")
    @ApiResponse(responseCode = "404", description = "Tank not found")
    @PutMapping("/{id}")
    public TankResponse update(@PathVariable Long id, @Valid @RequestBody TankRequest request) {
        return tankMapper.toResponse(tankService.updateTank(id, tankMapper.toEntity(request)));
    }
}
