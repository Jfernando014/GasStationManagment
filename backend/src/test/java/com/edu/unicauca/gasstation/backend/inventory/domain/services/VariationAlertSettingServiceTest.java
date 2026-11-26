package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.VariationAlertSetting;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.VariationAlertSettingRepository;
import com.edu.unicauca.gasstation.backend.inventory.exception.InvalidVariationAlertSettingException;
import com.edu.unicauca.gasstation.backend.inventory.exception.ProtectedVariationAlertSettingException;
import com.edu.unicauca.gasstation.backend.inventory.exception.VariationAlertSettingAlreadyExistsException;
import com.edu.unicauca.gasstation.backend.inventory.exception.VariationAlertSettingNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VariationAlertSettingServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-11-26T15:00:00Z"), BOGOTA);

    @Mock
    private VariationAlertSettingRepository repository;

    private VariationAlertSettingService service;

    @BeforeEach
    void setUp() {
        service = new VariationAlertSettingService(repository, FIXED_CLOCK);
    }

    @Test
    void createsValidSettingAndStampsCreatedAtFromClock() {
        VariationAlertSetting input = validSetting(99L, LocalDate.of(2026, 9, 1));
        when(repository.existsByValidFrom(LocalDate.of(2026, 9, 1))).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VariationAlertSetting saved = service.create(input);

        assertNull(saved.getId());
        assertEquals(LocalDateTime.now(FIXED_CLOCK), saved.getCreatedAt());
        verify(repository).save(input);
    }

    @Test
    void rejectsDuplicateValidityDateAndInvalidDomainRules() {
        VariationAlertSetting duplicate = validSetting(null, LocalDate.of(2026, 9, 1));
        when(repository.existsByValidFrom(LocalDate.of(2026, 9, 1))).thenReturn(true);
        assertThrows(VariationAlertSettingAlreadyExistsException.class, () -> service.create(duplicate));

        VariationAlertSetting zeroPct = validSetting(null, LocalDate.of(2026, 10, 1));
        zeroPct.setCumulativeLimitPercentage(BigDecimal.ZERO);
        assertThrows(InvalidVariationAlertSettingException.class, () -> service.create(zeroPct));

        VariationAlertSetting invalidDays = validSetting(null, LocalDate.of(2026, 10, 1));
        invalidDays.setEscalationDays(11);
        assertThrows(InvalidVariationAlertSettingException.class, () -> service.create(invalidDays));

        VariationAlertSetting nonMonotonic = validSetting(null, LocalDate.of(2026, 10, 1));
        nonMonotonic.setShortageLimit50To74(15); // larger than 1..49 limit (12)
        assertThrows(InvalidVariationAlertSettingException.class, () -> service.create(nonMonotonic));
    }

    @Test
    void protectsOldestSettingFromDeletionAndDateChangesWhileAllowingRuleEdits() {
        VariationAlertSetting oldest = validSetting(1L, LocalDate.of(2000, 1, 1));
        when(repository.findById(1L)).thenReturn(Optional.of(oldest));
        when(repository.findOldest()).thenReturn(Optional.of(oldest));

        assertThrows(ProtectedVariationAlertSettingException.class, () -> service.delete(1L));

        VariationAlertSetting movedDate = validSetting(null, LocalDate.of(2026, 1, 1));
        assertThrows(ProtectedVariationAlertSettingException.class, () -> service.update(1L, movedDate));

        VariationAlertSetting sameDateUpdate = validSetting(null, LocalDate.of(2000, 1, 1));
        sameDateUpdate.setCumulativeLimitPercentage(new BigDecimal("0.75"));
        when(repository.existsByValidFromAndIdNot(LocalDate.of(2000, 1, 1), 1L)).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VariationAlertSetting updated = service.update(1L, sameDateUpdate);
        assertEquals(new BigDecimal("0.75"), updated.getCumulativeLimitPercentage());
    }

    @Test
    void deletesNonOldestSettingAndThrowsWhenEffectiveSettingIsMissing() {
        VariationAlertSetting oldest = validSetting(1L, LocalDate.of(2000, 1, 1));
        VariationAlertSetting newer = validSetting(2L, LocalDate.of(2026, 8, 1));
        when(repository.findById(2L)).thenReturn(Optional.of(newer));
        when(repository.findOldest()).thenReturn(Optional.of(oldest));

        service.delete(2L);
        verify(repository).deleteById(2L);

        LocalDate queryDate = LocalDate.of(1999, 12, 31);
        when(repository.findEffectiveOn(queryDate)).thenReturn(Optional.empty());
        assertThrows(VariationAlertSettingNotFoundException.class, () -> service.getEffectiveOn(queryDate));
    }

    private static VariationAlertSetting validSetting(Long id, LocalDate validFrom) {
        VariationAlertSetting setting = new VariationAlertSetting();
        setting.setId(id);
        setting.setValidFrom(validFrom);
        setting.setCumulativeLimitPercentage(new BigDecimal("0.5"));
        setting.setEscalationDays(3);
        setting.setShortageLimit1To49(12);
        setting.setShortageLimit50To74(8);
        setting.setShortageLimit75To124(6);
        setting.setShortageLimit125To249(4);
        setting.setShortageLimit250To499(2);
        setting.setShortageLimit500OrMore(1);
        return setting;
    }
}
