package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.models.AdministrativeExpense;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Schema(description = "Administrative expense registered in the system")
public record AdministrativeExpenseResponseDto(
        Long id,
        LocalDate date,
        BigDecimal amount,
        String description,
        Instant createdAt) {

    public static AdministrativeExpenseResponseDto from(AdministrativeExpense expense) {
        return new AdministrativeExpenseResponseDto(
                expense.getId(),
                expense.getExpenseDate(),
                expense.getAmount(),
                expense.getDescription(),
                expense.getCreatedAt());
    }
}
