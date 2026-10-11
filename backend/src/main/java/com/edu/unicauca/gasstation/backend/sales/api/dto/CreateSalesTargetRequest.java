package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesTargetCategory;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesTargetUnit;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CreateSalesTargetRequest(
        @NotNull Integer year,
        @Min(1) @Max(12) Integer month,
        @NotNull SalesTargetCategory category,
        @NotNull @DecimalMin("0") BigDecimal value,
        @NotNull SalesTargetUnit unit) {
}
