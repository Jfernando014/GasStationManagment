package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.repositories;

import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities.AdministrativeExpenseEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministrativeExpenseJpaRepository extends JpaRepository<AdministrativeExpenseEntity, Long> {

    List<AdministrativeExpenseEntity> findAllByOrderByExpenseDateDescIdDesc();

    List<AdministrativeExpenseEntity> findByExpenseDateBetweenOrderByExpenseDateDescIdDesc(
            LocalDate fromDate, LocalDate toDate);
}