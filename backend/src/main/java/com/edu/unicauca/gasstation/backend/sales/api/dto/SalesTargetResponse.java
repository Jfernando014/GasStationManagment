package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesTargetCategory;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesTargetUnit;
import java.math.BigDecimal;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public record SalesTargetResponse(Long id, int year, Integer month, SalesTargetCategory category,
                                  BigDecimal value, SalesTargetUnit unit, Instant createdAt, BigDecimal maximumSale) {
    @JsonProperty("message")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String message() {
        return month == null ? "Metas del año guardadas correctamente" : null;
    }
}
