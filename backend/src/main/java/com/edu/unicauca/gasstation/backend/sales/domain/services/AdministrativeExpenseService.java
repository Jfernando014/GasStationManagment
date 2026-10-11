package com.edu.unicauca.gasstation.backend.sales.domain.services;

import com.edu.unicauca.gasstation.backend.sales.domain.models.AdministrativeExpense;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface AdministrativeExpenseService {

    AdministrativeExpense register(LocalDate date, BigDecimal amount, String description);

    List<AdministrativeExpense> listExpenses(Integer year, Integer month, Integer fortnight,
                                            LocalDate fromDate, LocalDate toDate);

    BigDecimal getTotalForFortnight(int year, int month, int fortnight);

    BigDecimal getTotalByDateRange(LocalDate fromDate, LocalDate toDate);

    void delete(Long id);
}
