package com.edu.unicauca.gasstation.backend.sales.domain.models;

import java.math.BigDecimal;
import java.time.LocalDate;

/** First date at which a ledger balance falls below zero. */
public record NegativeBalance(LocalDate date, BigDecimal balance) {
}
