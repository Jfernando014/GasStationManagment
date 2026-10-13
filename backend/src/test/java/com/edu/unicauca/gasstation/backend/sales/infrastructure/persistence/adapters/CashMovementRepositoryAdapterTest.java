package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.sales.domain.models.CashMovement;
import com.edu.unicauca.gasstation.backend.sales.domain.models.CashMovementType;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities.CashMovementEntity;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.repositories.CashMovementJpaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CashMovementRepositoryAdapterTest {

    @Mock
    private CashMovementJpaRepository jpaRepository;

    @InjectMocks
    private CashMovementRepositoryAdapter adapter;

    @Test
    void saveMapsAllFieldsBetweenDomainAndPersistence() {
        LocalDate date = LocalDate.of(2026, 8, 12);
        BigDecimal amount = new BigDecimal("125000.50");
        CashMovement movement = CashMovement.create(
                CashMovementType.DEPOSIT, date, amount, "Consignación diaria");
        when(jpaRepository.save(any(CashMovementEntity.class))).thenAnswer(invocation -> {
            CashMovementEntity entity = invocation.getArgument(0);
            return new CashMovementEntity(42L, entity.getMovementType(), entity.getMovementDate(),
                    entity.getAmount(), entity.getDescription());
        });

        CashMovement saved = adapter.save(movement);

        ArgumentCaptor<CashMovementEntity> captor = ArgumentCaptor.forClass(CashMovementEntity.class);
        verify(jpaRepository).save(captor.capture());
        assertEquals(CashMovementType.DEPOSIT, captor.getValue().getMovementType());
        assertEquals(date, captor.getValue().getMovementDate());
        assertEquals(amount, captor.getValue().getAmount());
        assertEquals("Consignación diaria", captor.getValue().getDescription());
        assertEquals(42L, saved.getId());
        assertEquals(CashMovementType.DEPOSIT, saved.getMovementType());
        assertEquals(date, saved.getMovementDate());
        assertEquals(amount, saved.getAmount());
        assertEquals("Consignación diaria", saved.getDescription());
    }
}