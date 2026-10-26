package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.AutonomyThreshold;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.AutonomyThresholdRepository;
import com.edu.unicauca.gasstation.backend.inventory.exception.AutonomyThresholdAlreadyExistsException;
import com.edu.unicauca.gasstation.backend.inventory.exception.InvalidAutonomyThresholdException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutonomyThresholdServiceTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-08-05T15:00:00Z"), ZoneId.of("America/Bogota"));
    private static final LocalDate FROM = LocalDate.of(2026, 8, 1);

    @Mock
    private AutonomyThresholdRepository repository;

    private AutonomyThresholdService service;

    @BeforeEach
    void setUp() {
        service = new AutonomyThresholdService(repository, CLOCK);
    }

    @Test
    void createSavesTheThresholdWithoutIdAndWithTheCreationTime() {
        AutonomyThreshold input = threshold(FROM, "5", "2");
        input.setId(99L);
        when(repository.existsByValidFrom(FROM)).thenReturn(false);
        when(repository.save(any(AutonomyThreshold.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AutonomyThreshold created = service.create(input);

        ArgumentCaptor<AutonomyThreshold> captor = ArgumentCaptor.forClass(AutonomyThreshold.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
        assertThat(created.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 8, 5, 10, 0));
        assertThat(created.getValidFrom()).isEqualTo(FROM);
    }

    @Test
    void createRejectsAMissingValidityDate() {
        assertThatThrownBy(() -> service.create(threshold(null, "5", "2")))
                .isInstanceOf(InvalidAutonomyThresholdException.class)
                .hasMessage("The validity date is required");
        verifyNoInteractions(repository);
    }

    @Test
    void createRejectsMissingThresholds() {
        assertThatThrownBy(() -> service.create(threshold(FROM, null, "2")))
                .isInstanceOf(InvalidAutonomyThresholdException.class)
                .hasMessage("The warning and critical thresholds are required");
        assertThatThrownBy(() -> service.create(threshold(FROM, "5", null)))
                .isInstanceOf(InvalidAutonomyThresholdException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void createRejectsACriticalThresholdThatIsNotGreaterThanZero() {
        assertThatThrownBy(() -> service.create(threshold(FROM, "5", "0")))
                .isInstanceOf(InvalidAutonomyThresholdException.class)
                .hasMessage("The critical threshold must be greater than zero");
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @CsvSource({"5,5", "5,6"})
    void createRejectsACriticalThresholdThatIsNotLowerThanTheWarning(String warning, String critical) {
        assertThatThrownBy(() -> service.create(threshold(FROM, warning, critical)))
                .isInstanceOf(InvalidAutonomyThresholdException.class)
                .hasMessage("The critical threshold must be lower than the warning threshold");
        verifyNoInteractions(repository);
    }

    @Test
    void createRejectsADuplicatedValidityDate() {
        when(repository.existsByValidFrom(FROM)).thenReturn(true);

        assertThatThrownBy(() -> service.create(threshold(FROM, "5", "2")))
                .isInstanceOf(AutonomyThresholdAlreadyExistsException.class)
                .hasMessage("A threshold with validity date 2026-08-01 already exists");
        verify(repository, never()).save(any());
    }

    @Test
    void findEffectiveOnDelegatesToTheRepository() {
        AutonomyThreshold threshold = threshold(FROM, "5", "2");
        when(repository.findEffectiveOn(LocalDate.of(2026, 8, 31))).thenReturn(Optional.of(threshold));

        assertThat(service.findEffectiveOn(LocalDate.of(2026, 8, 31))).contains(threshold);
    }

    @Test
    void findAllDelegatesToTheRepository() {
        List<AutonomyThreshold> thresholds = List.of(threshold(FROM, "5", "2"));
        when(repository.findAll()).thenReturn(thresholds);

        assertThat(service.findAll()).isEqualTo(thresholds);
    }

    private AutonomyThreshold threshold(LocalDate validFrom, String warning, String critical) {
        AutonomyThreshold threshold = new AutonomyThreshold();
        threshold.setValidFrom(validFrom);
        threshold.setWarningDays(warning == null ? null : new BigDecimal(warning));
        threshold.setCriticalDays(critical == null ? null : new BigDecimal(critical));
        return threshold;
    }
}