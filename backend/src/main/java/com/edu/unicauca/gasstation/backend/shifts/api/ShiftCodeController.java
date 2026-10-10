package com.edu.unicauca.gasstation.backend.shifts.api;

import com.edu.unicauca.gasstation.backend.shifts.api.dtos.ShiftCodeResponse;
import com.edu.unicauca.gasstation.backend.shifts.application.ShiftCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Shift catalog", description = "Read-only catalog of shift codes")
@RestController
@RequestMapping("/api/shifts/catalog")
@RequiredArgsConstructor
public class ShiftCodeController {

    private final ShiftCatalogService shiftCatalogService;

    @Operation(summary = "List active shift codes")
    @GetMapping
    public List<ShiftCodeResponse> listActive() {
        return shiftCatalogService.listActive();
    }

    @Operation(summary = "Get a shift code by its code (e.g. DIA6, 12-7)")
    @GetMapping("/{code}")
    public ShiftCodeResponse getByCode(@PathVariable String code) {
        return shiftCatalogService.getByCode(code);
    }
}
