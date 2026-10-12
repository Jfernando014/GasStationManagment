package com.edu.unicauca.gasstation.backend.sales.domain.services;

import com.edu.unicauca.gasstation.backend.sales.domain.models.AdministrativeExpense;
import com.edu.unicauca.gasstation.backend.sales.domain.models.ExpenseFilter;
import com.edu.unicauca.gasstation.backend.sales.domain.models.ExpenseListResult;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.AdministrativeExpenseRepository;
import com.edu.unicauca.gasstation.backend.sales.exception.AdministrativeExpenseNotFoundException;
import com.edu.unicauca.gasstation.backend.sales.exception.InvalidAdministrativeExpenseException;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class AdministrativeExpenseService {

    private final AdministrativeExpenseRepository repository;

    public AdministrativeExpenseService(AdministrativeExpenseRepository repository) {
        this.repository = repository;
    }

    public AdministrativeExpense register(LocalDate date, BigDecimal amount, String description) {
        if (date == null) {
            throw new InvalidAdministrativeExpenseException("La fecha del gasto es obligatoria");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAdministrativeExpenseException("El valor del gasto debe ser mayor que cero");
        }
        String normalizedDescription = description == null || description.isBlank()
                ? "Gasto administrativo"
                : description.strip();
        return repository.save(AdministrativeExpense.create(date, amount, normalizedDescription));
    }

    public ExpenseListResult list(ExpenseFilter filter) {
        ExpenseFilter selectedFilter = filter == null ? ExpenseFilter.all() : filter;
        DateRange range = dateRange(selectedFilter);
        List<AdministrativeExpense> expenses = range == null
            ? repository.findAll()
            : repository.findBetween(range.from(), range.to());
        return result(expenses);
    }

    public void delete(Long id) {
        if (id == null) {
            throw new InvalidAdministrativeExpenseException("El identificador del gasto es obligatorio");
        }
        if (repository.findById(id).isEmpty()) {
            throw new AdministrativeExpenseNotFoundException(id);
        }
        repository.deleteById(id);
    }

    private ExpenseListResult result(List<AdministrativeExpense> expenses) {
        BigDecimal total = expenses.stream()
                .map(AdministrativeExpense::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ExpenseListResult(expenses, total);
    }

    private static DateRange dateRange(ExpenseFilter filter) {
        boolean hasFortnightFilter = filter.year() != null || filter.month() != null || filter.fortnight() != null;
        boolean hasDateFilter = filter.date() != null || filter.fromDate() != null || filter.toDate() != null;
        if (hasFortnightFilter && hasDateFilter) {
            throw new InvalidAdministrativeExpenseException("Usa un filtro por quincena o por fecha, no ambos");
        }
        if (hasFortnightFilter) {
            if (filter.year() == null || filter.month() == null || filter.fortnight() == null) {
                throw new InvalidAdministrativeExpenseException("Año, mes y quincena son obligatorios");
            }
            if (filter.fortnight() != 1 && filter.fortnight() != 2) {
                throw new InvalidAdministrativeExpenseException("La quincena debe ser 1 o 2");
            }
            try {
                YearMonth month = YearMonth.of(filter.year(), filter.month());
                return filter.fortnight() == 1
                        ? new DateRange(month.atDay(1), month.atDay(Math.min(15, month.lengthOfMonth())))
                        : new DateRange(month.atDay(16), month.atEndOfMonth());
            } catch (DateTimeException ex) {
                throw new InvalidAdministrativeExpenseException("El año o mes no es válido");
            }
        }
        if (filter.date() != null) {
            if (filter.fromDate() != null || filter.toDate() != null) {
                throw new InvalidAdministrativeExpenseException("Usa una fecha exacta o un rango, no ambos");
            }
            return new DateRange(filter.date(), filter.date());
        }
        if (filter.fromDate() != null || filter.toDate() != null) {
            if (filter.fromDate() == null || filter.toDate() == null) {
                throw new InvalidAdministrativeExpenseException("Las dos fechas del rango son obligatorias");
            }
            if (filter.fromDate().isAfter(filter.toDate())) {
                throw new InvalidAdministrativeExpenseException("La fecha inicial debe ser anterior a la final");
            }
            return new DateRange(filter.fromDate(), filter.toDate());
        }
        return null;
    }

    private record DateRange(LocalDate from, LocalDate to) {
    }
}