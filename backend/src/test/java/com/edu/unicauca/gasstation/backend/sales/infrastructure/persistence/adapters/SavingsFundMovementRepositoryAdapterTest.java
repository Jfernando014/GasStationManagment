package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundMovement;
import com.edu.unicauca.gasstation.backend.sales.domain.models.SavingsFundMovementType;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities.SavingsFundMovementEntity;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.repositories.SavingsFundMovementJpaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SavingsFundMovementRepositoryAdapterTest {

    @Mock
    private SavingsFundMovementJpaRepository jpaRepository;

    @InjectMocks
    private SavingsFundMovementRepositoryAdapter adapter;

    @Test
    void saveMapsAllFieldsBetweenDomainAndPersistence() {
        LocalDate date = LocalDate.of(2026, 8, 12);
        BigDecimal amount = new BigDecimal("85000.25");
        SavingsFundMovement movement = SavingsFundMovement.create(
                SavingsFundMovementType.CONTRIBUTION, date, amount, "Aporte quincenal");
        when(jpaRepository.save(any(SavingsFundMovementEntity.class))).thenAnswer(invocation -> {
            SavingsFundMovementEntity entity = invocation.getArgument(0);
            return new SavingsFundMovementEntity(24L, entity.getMovementType(), entity.getMovementDate(),
                    entity.getAmount(), entity.getDescription());
        });

        SavingsFundMovement saved = adapter.save(movement);

        ArgumentCaptor<SavingsFundMovementEntity> captor = ArgumentCaptor.forClass(SavingsFundMovementEntity.class);
        verify(jpaRepository).save(captor.capture());
        assertEquals(SavingsFundMovementType.CONTRIBUTION, captor.getValue().getMovementType());
        assertEquals(date, captor.getValue().getMovementDate());
        assertEquals(amount, captor.getValue().getAmount());
        assertEquals("Aporte quincenal", captor.getValue().getDescription());
        assertEquals(24L, saved.getId());
        assertEquals(SavingsFundMovementType.CONTRIBUTION, saved.getMovementType());
        assertEquals(date, saved.getMovementDate());
        assertEquals(amount, saved.getAmount());
        assertEquals("Aporte quincenal", saved.getDescription());
    }
}