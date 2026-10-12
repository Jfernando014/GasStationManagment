package com.edu.unicauca.gasstation.backend.sales.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.edu.unicauca.gasstation.backend.sales.domain.models.AdministrativeExpense;
import com.edu.unicauca.gasstation.backend.sales.domain.models.ExpenseFilter;
import com.edu.unicauca.gasstation.backend.sales.domain.models.ExpenseListResult;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.AdministrativeExpenseRepository;
import com.edu.unicauca.gasstation.backend.sales.exception.AdministrativeExpenseNotFoundException;
import com.edu.unicauca.gasstation.backend.sales.exception.InvalidAdministrativeExpenseException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdministrativeExpenseServiceTest {

    private final List<AdministrativeExpense> store = new ArrayList<>();
    private AdministrativeExpenseService service;

    @BeforeEach
    void setUp() {
        AtomicLong ids = new AtomicLong();
        AdministrativeExpenseRepository repository = new AdministrativeExpenseRepository() {
            @Override
            public AdministrativeExpense save(AdministrativeExpense expense) {
                AdministrativeExpense saved = new AdministrativeExpense(ids.incrementAndGet(), expense.date(),
                        expense.amount(), expense.description(), Instant.now());
                store.add(saved);
                return saved;
            }

            @Override
            public List<AdministrativeExpense> findAll() {
                return store.stream().sorted((left, right) -> right.date().compareTo(left.date())).toList();
            }

            @Override
            public List<AdministrativeExpense> findBetween(LocalDate fromDate, LocalDate toDate) {
                return store.stream()
                        .filter(expense -> fromDate == null || !expense.date().isBefore(fromDate))
                        .filter(expense -> toDate == null || !expense.date().isAfter(toDate))
                        .sorted((left, right) -> right.date().compareTo(left.date()))
                        .toList();
            }

            @Override
            public Optional<AdministrativeExpense> findById(Long id) {
                return store.stream().filter(expense -> expense.id().equals(id)).findFirst();
            }

            @Override
            public void deleteById(Long id) {
                store.removeIf(expense -> expense.id().equals(id));
            }
        };
        service = new AdministrativeExpenseService(repository);
    }

    @Test
    void firstFortnightOfAugustTotals136100() {
        service.register(LocalDate.of(2026, 8, 1), new BigDecimal("80000.00"), "Servicios de energía");
        service.register(LocalDate.of(2026, 8, 10), new BigDecimal("25000.00"), "Limpieza y mantenimiento");
        service.register(LocalDate.of(2026, 8, 14), new BigDecimal("31100.00"), "Papelería y suministros");
        service.register(LocalDate.of(2026, 8, 16), new BigDecimal("99999.00"), "Quincena siguiente");

        ExpenseListResult result = service.list(new ExpenseFilter(2026, 8, 1, null, null, null));

        assertEquals(3, result.expenses().size());
        assertEquals(0, new BigDecimal("136100.00").compareTo(result.total()));
    }

    @Test
    void dateRangeFilterReturnsOnlyMatchingExpensesAndTotal() {
        service.register(LocalDate.of(2026, 8, 1), new BigDecimal("100.00"), null);
        service.register(LocalDate.of(2026, 8, 20), new BigDecimal("200.00"), null);

        ExpenseListResult result = service.list(new ExpenseFilter(null, null, null, null,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 15)));

        assertEquals(1, result.expenses().size());
        assertEquals(0, new BigDecimal("100.00").compareTo(result.total()));
    }

    @Test
    void rejectsInvalidAmountsAndIncompleteFortnightFilters() {
        assertThrows(InvalidAdministrativeExpenseException.class,
                () -> service.register(LocalDate.of(2026, 8, 1), BigDecimal.ZERO, null));
        assertThrows(InvalidAdministrativeExpenseException.class,
                () -> service.list(new ExpenseFilter(2026, 8, null, null, null, null)));
    }

    @Test
    void deletesExistingExpenseAndReportsMissingExpense() {
        AdministrativeExpense expense = service.register(LocalDate.of(2026, 8, 1),
                new BigDecimal("100.00"), "Papelería");

        service.delete(expense.id());

        assertEquals(0, service.list(ExpenseFilter.all()).expenses().size());
        assertThrows(AdministrativeExpenseNotFoundException.class, () -> service.delete(expense.id()));
    }
}