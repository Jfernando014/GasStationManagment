package com.edu.unicauca.gasstation.backend.sales.infrastructure.services;

import com.edu.unicauca.gasstation.backend.sales.domain.models.AdministrativeExpense;
import com.edu.unicauca.gasstation.backend.sales.domain.services.AdministrativeExpenseService;
import com.edu.unicauca.gasstation.backend.sales.exception.AdministrativeExpenseNotFoundException;
import com.edu.unicauca.gasstation.backend.sales.exception.InvalidAdministrativeExpenseException;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.AdministrativeExpenseRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdministrativeExpenseServiceImpl implements AdministrativeExpenseService {

    private final AdministrativeExpenseRepository repository;

    public AdministrativeExpenseServiceImpl(AdministrativeExpenseRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public AdministrativeExpense register(LocalDate date, BigDecimal amount, String description) {
        if (date == null) {
            throw new InvalidAdministrativeExpenseException("Expense date is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAdministrativeExpenseException("Expense amount must be greater than zero");
        }
        return repository.save(new AdministrativeExpense(date, amount, normalize(description)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdministrativeExpense> listExpenses(Integer year, Integer month, Integer fortnight,
                                                  LocalDate fromDate, LocalDate toDate) {
        if (fortnight != null) {
            if (year == null || month == null) {
                throw new InvalidAdministrativeExpenseException("Year and month are required when filtering by fortnight");
            }
            return listByFortnight(year, month, fortnight);
        }
        if (fromDate != null || toDate != null) {
            LocalDate start = fromDate != null ? fromDate : LocalDate.of(2000, 1, 1);
            LocalDate end = toDate != null ? toDate : LocalDate.of(2100, 12, 31);
            if (start.isAfter(end)) {
                throw new InvalidAdministrativeExpenseException("Start date must be before or equal to end date");
            }
            return repository.findByExpenseDateBetweenOrderByExpenseDateDescIdDesc(start, end);
        }
        return repository.findAllByOrderByExpenseDateDescIdDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalForFortnight(int year, int month, int fortnight) {
        return sum(listByFortnight(year, month, fortnight));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalByDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new InvalidAdministrativeExpenseException("Both dates are required for the total");
        }
        if (fromDate.isAfter(toDate)) {
            throw new InvalidAdministrativeExpenseException("From date must be before or equal to to date");
        }
        return sum(repository.findByExpenseDateBetweenOrderByExpenseDateDescIdDesc(fromDate, toDate));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (id == null) {
            throw new InvalidAdministrativeExpenseException("Expense id is required");
        }
        if (!repository.existsById(id)) {
            throw new AdministrativeExpenseNotFoundException(id);
        }
        repository.deleteById(id);
    }

    private List<AdministrativeExpense> listByFortnight(int year, int month, int fortnight) {
        if (fortnight != 1 && fortnight != 2) {
            throw new InvalidAdministrativeExpenseException("Fortnight must be 1 or 2");
        }
        int maxDay = LocalDate.of(year, month, 1).lengthOfMonth();
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = LocalDate.of(year, month, maxDay);
        if (fortnight == 1) {
            end = start.withDayOfMonth(Math.min(15, maxDay));
        } else {
            start = start.withDayOfMonth(16);
            end = LocalDate.of(year, month, maxDay);
        }
        return repository.findByExpenseDateBetweenOrderByExpenseDateDescIdDesc(start, end);
    }

    private BigDecimal sum(List<AdministrativeExpense> expenses) {
        return expenses.stream()
                .map(AdministrativeExpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String normalize(String description) {
        return (description == null || description.isBlank()) ? "Administrativo" : description.trim();
    }
}
