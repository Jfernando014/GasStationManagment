package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import com.edu.unicauca.gasstation.backend.inventory.ProductCategory;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
public class Product {
    private Long id;
    private String code;
    private String name;
    private ProductCategory category;
    private BigDecimal price;
    private BigDecimal cost;
    private Boolean active = true;

    /**
     * Margen sobre el precio en porcentaje. Vacio si el costo no se conoce.
     */
    public Optional<BigDecimal> calculateMarginPercent() {
        if (cost == null || price == null || price.signum() == 0) {
            return Optional.empty();
        }
        return Optional.of(price.subtract(cost)
                .multiply(BigDecimal.valueOf(100))
                .divide(price, 2, RoundingMode.HALF_UP));
    }
}
