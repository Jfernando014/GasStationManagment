package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** One line of the stock ledger. The quantity is signed: negative takes units out of the location. */
@Getter
@Setter
@NoArgsConstructor
public class StockMovement {

    private Long id;
    private Long productId;
    private LocalDate journalDate;
    private StockLocation location;
    private StockMovementType movementType;
    private Integer quantity;
    private String note;
    private LocalDateTime createdAt;
}