package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ProductResponse {
    private Long id;
    private String code;
    private String name;
    private ProductCategory category;
    private BigDecimal price;
    private BigDecimal cost;
    private boolean active;
    private BigDecimal marginPercent;
}