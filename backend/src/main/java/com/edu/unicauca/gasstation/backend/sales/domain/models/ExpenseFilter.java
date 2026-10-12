package com.edu.unicauca.gasstation.backend.sales.domain.models;

import java.time.LocalDate;

public record ExpenseFilter(
        Integer year,
        Integer month,
        Integer fortnight,
        LocalDate date,
        LocalDate fromDate,
        LocalDate toDate) {

    public static ExpenseFilter all() {
        return new ExpenseFilter(null, null, null, null, null, null);
    }
}