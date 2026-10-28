package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.StockStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Stock of a product on a journal date")
public class ProductStockResponse {

    private Long productId;
    private String productCode;
    private String productName;
    private ProductCategory category;
    private boolean active;
    private LocationStockResponse office;
    private LocationStockResponse counter;

    @Schema(description = "NEGATIVE_STOCK when the closing stock of a location is below zero")
    private StockStatus status;
}