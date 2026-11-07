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
@Schema(description = "Stock of a product at the end of a journal date")
public class ProductControlResponse {

    private Long productId;
    private String productCode;
    private String productName;
    private ProductCategory category;

    @Schema(description = "Units sold in the journal")
    private int sold;

    private int officeStock;
    private int counterStock;

    @Schema(description = "NEGATIVE_STOCK when a location closed below zero")
    private StockStatus status;

    @Schema(description = "Office plus counter stock is zero or less")
    private boolean depleted;
}