package com.edu.unicauca.gasstation.backend.sales.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A dated movement with its sign already applied (positive adds, negative subtracts). */
public record LedgerEntry(LocalDate date, BigDecimal signedAmount) {
}
