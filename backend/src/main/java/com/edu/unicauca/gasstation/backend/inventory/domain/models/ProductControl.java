package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductControl {

    private final Long productId;
    private final String productCode;
    private final String productName;
    private final ProductCategory category;
    private final int sold;
    private final int officeStock;
    private final int counterStock;
    private final StockStatus status;
    private final boolean depleted;
}