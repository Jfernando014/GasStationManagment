package com.edu.unicauca.gasstation.backend.sales.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A value of a sales parameter, effective from {@code validFrom} until a later value replaces it. */
public record SalesParameterValue(SalesParameterKey key, BigDecimal value, LocalDate validFrom) {
}
