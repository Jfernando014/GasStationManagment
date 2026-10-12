package com.edu.unicauca.gasstation.backend.sales.domain.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record AdministrativeExpense(
        Long id,
        LocalDate date,
        BigDecimal amount,
        String description,
        Instant createdAt) {

    public static AdministrativeExpense create(LocalDate date, BigDecimal amount, String description) {
        return new AdministrativeExpense(null, date, amount, description, null);
    }
}