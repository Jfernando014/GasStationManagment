package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.TankTestData;
import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Tank;
import com.edu.unicauca.gasstation.backend.inventory.exception.DuplicateTankCodeException;
import com.edu.unicauca.gasstation.backend.inventory.exception.TankNotFoundException;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.TankRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TankServiceTest {

    @Mock
    private TankRepository tankRepository;

    @InjectMocks
    private TankService tankService;

    @Test
    void createSavesTankWhenCodeIsAvailable() {
        Tank tank = TankTestData.tank(null, "T1C1", "Tanque 1 Comp. 1");
        when(tankRepository.existsByCode("T1C1")).thenReturn(false);
        when(tankRepository.save(tank)).thenReturn(tank);

        Tank created = tankService.createTank(tank);

        assertThat(created).isSameAs(tank);
        verify(tankRepository).save(tank);
    }

    @Test
    void createThrowsWhenCodeAlreadyExists() {
        Tank tank = TankTestData.tank(null, "T1C1", "Tanque 1 Comp. 1");
        when(tankRepository.existsByCode("T1C1")).thenReturn(true);

        assertThatThrownBy(() -> tankService.createTank(tank))
                .isInstanceOf(DuplicateTankCodeException.class)
                .hasMessageContaining("T1C1");
        verify(tankRepository, never()).save(any());
    }

    @Test
    void findByIdReturnsTankWhenItExists() {
        Tank tank = TankTestData.tank(1L, "T1C1", "Tanque 1 Comp. 1");
        when(tankRepository.findById(1L)).thenReturn(Optional.of(tank));

        assertThat(tankService.findTankById(1L)).isSameAs(tank);
    }

    @Test
    void findByIdThrowsWhenTankDoesNotExist() {
        when(tankRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tankService.findTankById(99L))
                .isInstanceOf(TankNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void findAllReturnsTanksOrderedByCode() {
        Tank first = TankTestData.tank(1L, "T1C1", "Tanque 1 Comp. 1");
        Tank second = TankTestData.tank(2L, "T1C2", "Tanque 1 Comp. 2");
        when(tankRepository.findAllOrderByCode()).thenReturn(List.of(first, second));

        assertThat(tankService.findAllTanks()).containsExactly(first, second);
    }

    @Test
    void updateChangesEditableFieldsIncludingCode() {
        Tank existing = TankTestData.tank(1L, "T1C1", "Nombre anterior");
        Tank changes = TankTestData.tank(null, "OTRO", "Nombre nuevo");

        changes.setFuelType(FuelType.DIESEL);
        changes.setMaxCapacityGallons(new BigDecimal("3000"));
        changes.setMaxHeightCm(new BigDecimal("200"));
        changes.setToleranceCm(new BigDecimal("2"));

        when(tankRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(tankRepository.existsByCode("OTRO")).thenReturn(false);
        when(tankRepository.save(existing)).thenReturn(existing);

        Tank updated = tankService.updateTank(1L, changes);

        assertThat(updated.getCode()).isEqualTo("OTRO");
        assertThat(updated.getName()).isEqualTo("Nombre nuevo");
        assertThat(updated.getFuelType()).isEqualTo(FuelType.DIESEL);
        assertThat(updated.getMaxCapacityGallons()).isEqualByComparingTo("3000");
        assertThat(updated.getMaxHeightCm()).isEqualByComparingTo("200");
        assertThat(updated.getToleranceCm()).isEqualByComparingTo("2");
    }

    @Test
    void updateThrowsWhenTankDoesNotExist() {
        when(tankRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tankService.updateTank(99L, TankTestData.tank(null, "T1C1", "Nuevo")))
                .isInstanceOf(TankNotFoundException.class);
        verify(tankRepository, never()).save(any());
    }

    @Test
    void updateThrowsWhenNewCodeAlreadyExists() {
        Tank existing = TankTestData.tank(1L, "T1C1", "Tanque 1");
        Tank changes = TankTestData.tank(null, "T1C2", "Tanque actualizado");

        when(tankRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(tankRepository.existsByCode("T1C2")).thenReturn(true);

        assertThatThrownBy(() -> tankService.updateTank(1L, changes))
                .isInstanceOf(DuplicateTankCodeException.class)
                .hasMessageContaining("T1C2");

        verify(tankRepository, never()).save(any());
    }
}