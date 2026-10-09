package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.FuelPriceInfo;
import com.edu.unicauca.gasstation.backend.inventory.FuelPriceTestData;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelPrice;
import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.exception.FuelPriceNotFoundException;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.FuelPriceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuelPriceServiceTest {

    private static final LocalDate AUGUST_1 = LocalDate.of(2026, 8, 1);
    private static final LocalDate AUGUST_5 = LocalDate.of(2026, 8, 5);

    @Mock
    private FuelPriceRepository fuelPriceRepository;

    @InjectMocks
    private FuelPriceService fuelPriceService;

    @Test
    void createSavesThePrice() {
        FuelPrice price = FuelPriceTestData.price(null, FuelType.MOTOR, "16330", AUGUST_1);
        when(fuelPriceRepository.save(price)).thenReturn(price);

        assertThat(fuelPriceService.create(price)).isSameAs(price);
    }

    @Test
    void updateChangesAllFields() {
        FuelPrice existing = FuelPriceTestData.price(1L, FuelType.MOTOR, "16330", AUGUST_1);
        FuelPrice changes = FuelPriceTestData.price(null, FuelType.EXTRA, "20390", AUGUST_5);
        when(fuelPriceRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(fuelPriceRepository.saveAndFlush(existing)).thenReturn(existing);

        FuelPrice updated = fuelPriceService.update(1L, changes);

        assertThat(updated.getFuelType()).isEqualTo(FuelType.EXTRA);
        assertThat(updated.getPricePerGallon()).isEqualByComparingTo("20390");
        assertThat(updated.getValidFrom()).isEqualTo(AUGUST_5);
    }

    @Test
    void updateThrowsWhenThePriceDoesNotExist() {
        when(fuelPriceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fuelPriceService.update(99L,
                FuelPriceTestData.price(null, FuelType.MOTOR, "16330", AUGUST_1)))
                .isInstanceOf(FuelPriceNotFoundException.class)
                .hasMessageContaining("99");
        verify(fuelPriceRepository, never()).saveAndFlush(any());
    }

    @Test
    void findEffectiveReturnsThePriceFromTheRepository() {
        FuelPrice price = FuelPriceTestData.price(1L, FuelType.MOTOR, "16330", AUGUST_1);
        when(fuelPriceRepository.findFirstByFuelTypeAndValidFromLessThanEqualOrderByValidFromDesc(
                FuelType.MOTOR, AUGUST_5)).thenReturn(Optional.of(price));

        assertThat(fuelPriceService.findEffective(FuelType.MOTOR, AUGUST_5)).isSameAs(price);
    }

    @Test
    void findEffectiveThrowsWhenNoPriceHasStartedYet() {
        when(fuelPriceRepository.findFirstByFuelTypeAndValidFromLessThanEqualOrderByValidFromDesc(
                FuelType.MOTOR, AUGUST_1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fuelPriceService.findEffective(FuelType.MOTOR, AUGUST_1))
                .isInstanceOf(FuelPriceNotFoundException.class);
    }

    @Test
    void findHistoryFiltersByFuelTypeWhenProvided() {
        FuelPrice price = FuelPriceTestData.price(1L, FuelType.EXTRA, "20390", AUGUST_1);
        when(fuelPriceRepository.findByFuelTypeOrderByValidFromDesc(FuelType.EXTRA)).thenReturn(List.of(price));

        assertThat(fuelPriceService.findHistory(FuelType.EXTRA)).containsExactly(price);
    }
    @Test
    void findEffectivePriceReturnsPublicInfoOfThePriceInForce() {
        FuelPrice price = FuelPriceTestData.price(1L, FuelType.MOTOR, "16330", AUGUST_1);
        when(fuelPriceRepository.findFirstByFuelTypeAndValidFromLessThanEqualOrderByValidFromDesc(
                FuelType.MOTOR, AUGUST_5)).thenReturn(Optional.of(price));

        Optional<FuelPriceInfo> info = fuelPriceService.findEffectivePrice(FuelType.MOTOR, AUGUST_5);

        assertThat(info).isPresent();
        assertThat(info.get().getFuelType()).isEqualTo(FuelType.MOTOR);
        assertThat(info.get().getPricePerGallon()).isEqualByComparingTo("16330");
        assertThat(info.get().getValidFrom()).isEqualTo(AUGUST_1);
    }

    @Test
    void findEffectivePriceIsEmptyWhenNoPriceHasStarted() {
        when(fuelPriceRepository.findFirstByFuelTypeAndValidFromLessThanEqualOrderByValidFromDesc(
                FuelType.MOTOR, AUGUST_1)).thenReturn(Optional.empty());

        assertThat(fuelPriceService.findEffectivePrice(FuelType.MOTOR, AUGUST_1)).isEmpty();
    }

    @Test
    void findEffectivePriceRejectsNullArguments() {
        assertThatThrownBy(() -> fuelPriceService.findEffectivePrice(null, AUGUST_5))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> fuelPriceService.findEffectivePrice(FuelType.MOTOR, null))
                .isInstanceOf(NullPointerException.class);
    }
}