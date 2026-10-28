package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockLocation;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockMovementType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class StockMovementResponse {

    private Long id;
    private Long productId;
    private LocalDate journalDate;
    private StockLocation location;
    private StockMovementType movementType;

    @Schema(description = "Signed units: negative takes units out of the location")
    private Integer quantity;

    private String note;
    private LocalDateTime createdAt;
}