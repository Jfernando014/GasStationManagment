package com.edu.unicauca.gasstation.backend.sales.domain.models;

import java.math.BigDecimal;
import java.util.List;

public record ExpenseListResult(List<AdministrativeExpense> expenses, BigDecimal total) {

    public ExpenseListResult {
        expenses = List.copyOf(expenses);
    }
}