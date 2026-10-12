package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.models.ExpenseListResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Expenses matching the selected filter and their total")
public record AdministrativeExpenseListResponseDto(
        List<AdministrativeExpenseResponseDto> expenses,
        BigDecimal total) {

    public AdministrativeExpenseListResponseDto {
        expenses = List.copyOf(expenses);
    }

    public static AdministrativeExpenseListResponseDto from(ExpenseListResult result) {
        return new AdministrativeExpenseListResponseDto(
                result.expenses().stream().map(AdministrativeExpenseResponseDto::from).toList(), result.total());
    }
}