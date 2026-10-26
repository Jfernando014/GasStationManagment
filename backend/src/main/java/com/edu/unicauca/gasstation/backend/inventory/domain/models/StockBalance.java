package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Units of a product in a location. */
@Getter
@AllArgsConstructor
public class StockBalance {

    private final Long productId;
    private final StockLocation location;
    private final int quantity;
}