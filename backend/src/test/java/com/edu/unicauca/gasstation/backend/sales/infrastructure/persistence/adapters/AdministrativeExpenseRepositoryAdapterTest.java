package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.sales.domain.models.AdministrativeExpense;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities.AdministrativeExpenseEntity;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.repositories.AdministrativeExpenseJpaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdministrativeExpenseRepositoryAdapterTest {

    @Mock
    private AdministrativeExpenseJpaRepository jpaRepository;

    @InjectMocks
    private AdministrativeExpenseRepositoryAdapter adapter;

    @Test
    void saveMapsAllFieldsBetweenDomainAndPersistence() {
        LocalDate date = LocalDate.of(2026, 8, 12);
        BigDecimal amount = new BigDecimal("136100.00");
        Instant createdAt = Instant.parse("2026-08-12T15:30:00Z");
        AdministrativeExpense expense = AdministrativeExpense.create(date, amount, "Servicios públicos");
        when(jpaRepository.save(any(AdministrativeExpenseEntity.class))).thenAnswer(invocation -> {
            AdministrativeExpenseEntity entity = invocation.getArgument(0);
            entity.setId(18L);
            entity.setCreatedAt(createdAt);
            return entity;
        });

        AdministrativeExpense saved = adapter.save(expense);

        ArgumentCaptor<AdministrativeExpenseEntity> captor = ArgumentCaptor.forClass(AdministrativeExpenseEntity.class);
        verify(jpaRepository).save(captor.capture());
        assertEquals(date, captor.getValue().getExpenseDate());
        assertEquals(amount, captor.getValue().getAmount());
        assertEquals("Servicios públicos", captor.getValue().getDescription());
        assertEquals(18L, saved.id());
        assertEquals(date, saved.date());
        assertEquals(amount, saved.amount());
        assertEquals("Servicios públicos", saved.description());
        assertEquals(createdAt, saved.createdAt());
    }
}