package com.edu.unicauca.gasstation.backend.sales.domain.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record AdministrativeExpense(
        Long id,
        LocalDate date,
        BigDecimal amount,
        String name,
        String documentNumber,
        String description,
        Instant createdAt) {

    public AdministrativeExpense(Long id, LocalDate date, BigDecimal amount, String description,
            Instant createdAt) {
        this(id, date, amount, null, null, description, createdAt);
    }

    public static AdministrativeExpense create(LocalDate date, BigDecimal amount, String description) {
        return create(date, amount, null, null, description);
    }

    public static AdministrativeExpense create(LocalDate date, BigDecimal amount, String name,
            String documentNumber, String description) {
        return new AdministrativeExpense(null, date, amount, name, documentNumber, description, null);
    }

    /** Formats supplier name and concept cleanly for daily cash balance summaries. */
    public String displayDescription() {
        return name != null && !name.isBlank() ? name + " - " + description : description;
    }
}