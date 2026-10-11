package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesTargetCategory;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesTargetUnit;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnore;

public record CreateSalesTargetRequest(
        @NotNull Integer year,
        @Min(1) @Max(12) Integer month,
        @NotNull SalesTargetCategory category,
        @NotNull @DecimalMin("0") BigDecimal value,
        @NotNull SalesTargetUnit unit,
        @DecimalMin("0") BigDecimal maximumSale) {
    public CreateSalesTargetRequest(Integer year, Integer month, SalesTargetCategory category, BigDecimal value, SalesTargetUnit unit) {
        this(year, month, category, value, unit, null);
    }

    @JsonIgnore
    @AssertTrue(message = "maximumSale is required for a new annual target")
    public boolean isAnnualMaximumProvided() { return month != null || maximumSale != null; }
}
