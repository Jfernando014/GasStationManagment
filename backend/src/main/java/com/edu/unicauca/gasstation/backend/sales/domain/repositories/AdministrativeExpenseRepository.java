package com.edu.unicauca.gasstation.backend.sales.domain.repositories;

import com.edu.unicauca.gasstation.backend.sales.domain.models.AdministrativeExpense;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AdministrativeExpenseRepository {

    AdministrativeExpense save(AdministrativeExpense expense);

    List<AdministrativeExpense> findAll();

    List<AdministrativeExpense> findBetween(LocalDate fromDate, LocalDate toDate);

    Optional<AdministrativeExpense> findById(Long id);

    void deleteById(Long id);
}