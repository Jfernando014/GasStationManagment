package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence;

import com.edu.unicauca.gasstation.backend.sales.domain.models.AdministrativeExpense;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdministrativeExpenseRepository extends JpaRepository<AdministrativeExpense, Long> {

    List<AdministrativeExpense> findByExpenseDateBetweenOrderByExpenseDateDescIdDesc(LocalDate start, LocalDate end);

    List<AdministrativeExpense> findAllByOrderByExpenseDateDescIdDesc();
}
