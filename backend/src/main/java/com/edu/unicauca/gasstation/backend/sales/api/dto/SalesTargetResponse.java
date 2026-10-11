package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesTargetCategory;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SalesTargetUnit;
import java.math.BigDecimal;
import java.time.Instant;

public record SalesTargetResponse(Long id, int year, Integer month, SalesTargetCategory category,
                                  BigDecimal value, SalesTargetUnit unit, Instant createdAt) {
}
