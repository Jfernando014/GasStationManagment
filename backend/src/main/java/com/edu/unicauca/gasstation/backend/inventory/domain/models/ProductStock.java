package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductStock {

    private final Long productId;
    private final String productCode;
    private final String productName;
    private final ProductCategory category;
    private final boolean active;
    private final LocationStock office;
    private final LocationStock counter;
    private final StockStatus status;
}