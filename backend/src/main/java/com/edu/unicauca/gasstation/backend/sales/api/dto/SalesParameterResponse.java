package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesParameterValue;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesParameterResponse(String key, BigDecimal value, LocalDate validFrom) {
    public static SalesParameterResponse from(SalesParameterValue v) {
        return new SalesParameterResponse(v.key().slug(), v.value(), v.validFrom());
    }
}
