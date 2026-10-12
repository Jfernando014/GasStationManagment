package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.sales.domain.models.AdministrativeExpense;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.AdministrativeExpenseRepository;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities.AdministrativeExpenseEntity;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.repositories.AdministrativeExpenseJpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class AdministrativeExpenseRepositoryAdapter implements AdministrativeExpenseRepository {

    private final AdministrativeExpenseJpaRepository jpaRepository;

    public AdministrativeExpenseRepositoryAdapter(AdministrativeExpenseJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public AdministrativeExpense save(AdministrativeExpense expense) {
        AdministrativeExpenseEntity entity = new AdministrativeExpenseEntity();
        entity.setExpenseDate(expense.date());
        entity.setAmount(expense.amount());
        entity.setDescription(expense.description());
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdministrativeExpense> findAll() {
        return jpaRepository.findAllByOrderByExpenseDateDescIdDesc().stream()
                .map(AdministrativeExpenseRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdministrativeExpense> findBetween(LocalDate fromDate, LocalDate toDate) {
        return jpaRepository.findByExpenseDateBetweenOrderByExpenseDateDescIdDesc(fromDate, toDate).stream()
                .map(AdministrativeExpenseRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdministrativeExpense> findById(Long id) {
        return jpaRepository.findById(id).map(AdministrativeExpenseRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    private static AdministrativeExpense toDomain(AdministrativeExpenseEntity entity) {
        return new AdministrativeExpense(entity.getId(), entity.getExpenseDate(), entity.getAmount(),
                entity.getDescription(), entity.getCreatedAt());
    }
}