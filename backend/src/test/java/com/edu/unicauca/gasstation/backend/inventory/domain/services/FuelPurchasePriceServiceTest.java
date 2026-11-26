package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.inventory.FuelPurchasePriceInfo;
import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelPurchasePrice;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.FuelPurchasePriceRepository;
import com.edu.unicauca.gasstation.backend.inventory.exception.FuelPurchasePriceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FuelPurchasePriceServiceTest {

    @Mock
    private FuelPurchasePriceRepository repository;

    @InjectMocks
    private FuelPurchasePriceService service;

    @Test
    void updatesExistingPriceAndThrowsNotFoundForUnknownId() {
        FuelPurchasePrice existing = price(10L, FuelType.MOTOR, "13800", LocalDate.of(2026, 8, 1));
        FuelPurchasePrice changes = price(null, FuelType.MOTOR, "14100", LocalDate.of(2026, 8, 10));
        when(repository.findById(10L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FuelPurchasePrice updated = service.update(10L, changes);

        assertEquals(new BigDecimal("14100"), updated.getPricePerGallon());
        assertEquals(LocalDate.of(2026, 8, 10), updated.getValidFrom());

        when(repository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(FuelPurchasePriceNotFoundException.class, () -> service.update(999L, changes));
    }

    @Test
    void exposesEffectivePurchasePriceThroughDomainAndQueryPort() {
        LocalDate date = LocalDate.of(2026, 8, 15);
        FuelPurchasePrice effective = price(1L, FuelType.DIESEL, "9800", LocalDate.of(2026, 8, 1));
        when(repository.findEffective(FuelType.DIESEL, date)).thenReturn(Optional.of(effective));

        assertEquals(effective, service.findEffective(FuelType.DIESEL, date));

        Optional<FuelPurchasePriceInfo> info = service.findEffectivePrice(FuelType.DIESEL, date);
        assertTrue(info.isPresent());
        assertEquals(new BigDecimal("9800"), info.get().getPricePerGallon());
        assertEquals(LocalDate.of(2026, 8, 1), info.get().getValidFrom());

        when(repository.findEffective(FuelType.EXTRA, date)).thenReturn(Optional.empty());
        assertTrue(service.findEffectivePrice(FuelType.EXTRA, date).isEmpty());
        assertThrows(FuelPurchasePriceNotFoundException.class, () -> service.findEffective(FuelType.EXTRA, date));
        assertThrows(NullPointerException.class, () -> service.findEffectivePrice(null, date));
        assertThrows(NullPointerException.class, () -> service.findEffectivePrice(FuelType.MOTOR, null));
    }

    @Test
    void delegatesHistoryToFullOrFuelFilteredQuery() {
        List<FuelPurchasePrice> all = List.of(price(1L, FuelType.MOTOR, "14000", LocalDate.of(2026, 8, 1)));
        when(repository.findHistory()).thenReturn(all);
        when(repository.findHistory(FuelType.MOTOR)).thenReturn(all);

        assertEquals(all, service.findHistory(null));
        assertEquals(all, service.findHistory(FuelType.MOTOR));
        verify(repository).findHistory();
        verify(repository).findHistory(FuelType.MOTOR);
    }

    private static FuelPurchasePrice price(Long id, FuelType fuelType, String amount, LocalDate validFrom) {
        FuelPurchasePrice p = new FuelPurchasePrice();
        p.setId(id);
        p.setFuelType(fuelType);
        p.setPricePerGallon(new BigDecimal(amount));
        p.setValidFrom(validFrom);
        return p;
    }
}
