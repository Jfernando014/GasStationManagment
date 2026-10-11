package com.edu.unicauca.gasstation.backend.sales.infrastructure.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import com.edu.unicauca.gasstation.backend.sales.domain.models.AdministrativeExpense;
import com.edu.unicauca.gasstation.backend.sales.exception.InvalidAdministrativeExpenseException;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.AdministrativeExpenseRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdministrativeExpenseServiceTest {

    @Mock
    private AdministrativeExpenseRepository repository;

    private final List<AdministrativeExpense> store = new ArrayList<>();
    private AdministrativeExpenseServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(repository.save(any(AdministrativeExpense.class))).thenAnswer(invocation -> {
            AdministrativeExpense e = invocation.getArgument(0);
            store.add(e);
            return e;
        });
        lenient().when(repository.findAllByOrderByExpenseDateDescIdDesc()).thenAnswer(invocation -> store.stream()
                .sorted((a, b) -> b.getExpenseDate().compareTo(a.getExpenseDate()))
                .toList());
        lenient().when(repository.findByExpenseDateBetweenOrderByExpenseDateDescIdDesc(any(), any())).thenAnswer(invocation -> {
            LocalDate start = invocation.getArgument(0);
            LocalDate end = invocation.getArgument(1);
            return store.stream()
                    .filter(e -> !e.getExpenseDate().isBefore(start) && !e.getExpenseDate().isAfter(end))
                    .sorted((a, b) -> b.getExpenseDate().compareTo(a.getExpenseDate()))
                    .toList();
        });
        service = new AdministrativeExpenseServiceImpl(repository);
    }

    private static BigDecimal cop(long value) {
        return BigDecimal.valueOf(value);
    }

    @Test
    void totalsExpensesByFortnightAndFiltersByDate() {
        service.register(LocalDate.of(2026, 8, 1), cop(80_000), "Servicios");
        service.register(LocalDate.of(2026, 8, 10), cop(25_000), "Limpieza");
        service.register(LocalDate.of(2026, 8, 16), cop(31_100), "Internet");

        assertEquals(0, cop(105_000).compareTo(service.getTotalForFortnight(2026, 8, 1)));
        assertEquals(0, cop(31_100).compareTo(service.getTotalForFortnight(2026, 8, 2)));
        assertEquals(0, cop(105_000).compareTo(service.getTotalByDateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 15))));
    }

    @Test
    void rejectZeroOrNegativeAmount() {
        assertThrows(InvalidAdministrativeExpenseException.class,
                () -> service.register(LocalDate.of(2026, 8, 1), BigDecimal.ZERO, "Invalid"));
        assertThrows(InvalidAdministrativeExpenseException.class,
                () -> service.register(LocalDate.of(2026, 8, 1), cop(-1), "Invalid"));
    }
}

