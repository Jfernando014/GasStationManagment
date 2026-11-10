package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.InspectionCategory;
import com.edu.unicauca.gasstation.backend.inventory.InspectionStatus;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.Inspection;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.InspectionRepository;
import com.edu.unicauca.gasstation.backend.inventory.exception.DuplicateInspectionCodeException;
import com.edu.unicauca.gasstation.backend.inventory.exception.InspectionNotFoundException;
import com.edu.unicauca.gasstation.backend.inventory.exception.InvalidInspectionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static com.edu.unicauca.gasstation.backend.inventory.InspectionTestData.inspection;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InspectionServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 5);
    private static final LocalDate EXECUTED = LocalDate.of(2026, 3, 15);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-05T15:00:00Z"),
            ZoneId.of("America/Bogota"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 10, 0);

    @Mock
    private InspectionRepository inspectionRepository;

    private InspectionService service;

    @BeforeEach
    void setUp() {
        service = new InspectionService(inspectionRepository, CLOCK);
    }

    // ---------- create ----------

    @Test
    void createStoresTheInspectionWithTimestampsAndTrimmedData() {
        when(inspectionRepository.save(any(Inspection.class))).thenAnswer(inv -> inv.getArgument(0));
        Inspection request = inspection(99L, "  CAL-ONAC-2026-08  ", InspectionCategory.ONAC,
                EXECUTED, TODAY.plusMonths(6));
        request.setTarget("  Dispensador 1 y 2  ");
        request.setCertifier("  Metrología del Cauca SAS  ");
        request.setToleranceMeasured("  +3,8 mL  ");
        request.setCertDoc("  CERT-ONAC-0812.pdf  ");

        Inspection created = service.create(request);

        assertThat(created.getId()).isNull();
        assertThat(created.getCode()).isEqualTo("CAL-ONAC-2026-08");
        assertThat(created.getTarget()).isEqualTo("Dispensador 1 y 2");
        assertThat(created.getCertifier()).isEqualTo("Metrología del Cauca SAS");
        assertThat(created.getToleranceMeasured()).isEqualTo("+3,8 mL");
        assertThat(created.getCertDoc()).isEqualTo("CERT-ONAC-0812.pdf");
        assertThat(created.getCreatedAt()).isEqualTo(NOW);
        assertThat(created.getUpdatedAt()).isEqualTo(NOW);
        assertThat(created.getStatus()).isEqualTo(InspectionStatus.VIGENTE);
    }

    @Test
    void createClearsABlankCertificateDocument() {
        when(inspectionRepository.save(any(Inspection.class))).thenAnswer(inv -> inv.getArgument(0));
        Inspection request = inspection(null, "CAL-ONAC-2026-08", InspectionCategory.ONAC,
                EXECUTED, TODAY.plusMonths(6));
        request.setCertDoc("   ");

        assertThat(service.create(request).getCertDoc()).isNull();
    }

    @Test
    void createRejectsAFutureExecutionDate() {
        Inspection request = inspection(null, "CAL-ONAC-2026-08", InspectionCategory.ONAC,
                TODAY.plusDays(1), TODAY.plusMonths(6));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(InvalidInspectionException.class)
                .hasMessage("The execution date is not valid");
        verify(inspectionRepository, never()).save(any());
    }

    @Test
    void createRejectsADueDateThatIsNotAfterTheExecutionDate() {
        Inspection request = inspection(null, "CAL-ONAC-2026-08", InspectionCategory.ONAC,
                EXECUTED, EXECUTED);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(InvalidInspectionException.class)
                .hasMessage("The due date must be after the execution date");
        verify(inspectionRepository, never()).save(any());
    }

    @Test
    void createRejectsADuplicateCode() {
        when(inspectionRepository.existsByCode("CAL-ONAC-2026-08")).thenReturn(true);
        Inspection request = inspection(null, "CAL-ONAC-2026-08", InspectionCategory.ONAC,
                EXECUTED, TODAY.plusMonths(6));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(DuplicateInspectionCodeException.class)
                .hasMessageContaining("CAL-ONAC-2026-08");
        verify(inspectionRepository, never()).save(any());
    }

    // ---------- update ----------

    @Test
    void updateKeepsTheCreationTimeAndRecomputesTheStatus() {
        Inspection existing = inspection(7L, "CAL-ONAC-2026-08", InspectionCategory.ONAC,
                EXECUTED, TODAY.plusMonths(6));
        when(inspectionRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(inspectionRepository.save(existing)).thenReturn(existing);
        Inspection changes = inspection(null, "CAL-ONAC-2026-09", InspectionCategory.HERMETICIDAD,
                EXECUTED, TODAY.minusDays(1));

        Inspection updated = service.update(7L, changes);

        assertThat(updated.getId()).isEqualTo(7L);
        assertThat(updated.getCode()).isEqualTo("CAL-ONAC-2026-09");
        assertThat(updated.getCategory()).isEqualTo(InspectionCategory.HERMETICIDAD);
        assertThat(updated.getCreatedAt()).isEqualTo(EXECUTED.atTime(8, 0));
        assertThat(updated.getUpdatedAt()).isEqualTo(NOW);
        assertThat(updated.getStatus()).isEqualTo(InspectionStatus.VENCIDO);
    }

    @Test
    void updateExcludesTheInspectionItselfFromTheDuplicateCheck() {
        Inspection existing = inspection(7L, "CAL-ONAC-2026-08", InspectionCategory.ONAC,
                EXECUTED, TODAY.plusMonths(6));
        when(inspectionRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(inspectionRepository.save(existing)).thenReturn(existing);
        Inspection changes = inspection(null, "CAL-ONAC-2026-08", InspectionCategory.ONAC,
                EXECUTED, TODAY.plusMonths(6));

        service.update(7L, changes);

        verify(inspectionRepository).existsByCodeAndIdNot("CAL-ONAC-2026-08", 7L);
    }

    @Test
    void updateRejectsACodeUsedByAnotherInspection() {
        Inspection existing = inspection(7L, "CAL-ONAC-2026-08", InspectionCategory.ONAC,
                EXECUTED, TODAY.plusMonths(6));
        when(inspectionRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(inspectionRepository.existsByCodeAndIdNot("CAL-ONAC-2026-09", 7L)).thenReturn(true);
        Inspection changes = inspection(null, "CAL-ONAC-2026-09", InspectionCategory.ONAC,
                EXECUTED, TODAY.plusMonths(6));

        assertThatThrownBy(() -> service.update(7L, changes))
                .isInstanceOf(DuplicateInspectionCodeException.class);
        verify(inspectionRepository, never()).save(any());
    }

    @Test
    void updateThrowsWhenTheInspectionDoesNotExist() {
        when(inspectionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L,
                inspection(null, "CAL-ONAC-2026-08", InspectionCategory.ONAC, EXECUTED, TODAY.plusMonths(6))))
                .isInstanceOf(InspectionNotFoundException.class);
        verify(inspectionRepository, never()).save(any());
    }

    // ---------- queries ----------

    @Test
    void findByIdReturnsTheInspectionWithItsStatus() {
        Inspection existing = inspection(7L, "CAL-ONAC-2026-08", InspectionCategory.ONAC,
                EXECUTED, TODAY.plusDays(10));
        when(inspectionRepository.findById(7L)).thenReturn(Optional.of(existing));

        Inspection found = service.findById(7L);

        assertThat(found).isSameAs(existing);
        assertThat(found.getStatus()).isEqualTo(InspectionStatus.PROXIMO);
    }

    @Test
    void findByIdThrowsWhenTheInspectionDoesNotExist() {
        when(inspectionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(InspectionNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void searchDelegatesTheCategoryAndDatesAndFiltersByStatus() {
        LocalDate from = TODAY.minusMonths(6);
        Inspection valid = inspection(1L, "CAL-ONAC-2026-08", InspectionCategory.ONAC, EXECUTED,
                TODAY.plusMonths(6));
        Inspection expired = inspection(2L, "HERM-TK-2026-01", InspectionCategory.HERMETICIDAD, EXECUTED,
                TODAY.minusDays(1));
        when(inspectionRepository.search(InspectionCategory.ONAC, from, TODAY)).thenReturn(List.of(valid, expired));

        assertThat(service.search(InspectionCategory.ONAC, InspectionStatus.VIGENTE, from, TODAY))
                .containsExactly(valid);
    }

    @Test
    void searchWithoutStatusReturnsEverythingWithItsComputedStatus() {
        Inspection valid = inspection(1L, "CAL-ONAC-2026-08", InspectionCategory.ONAC, EXECUTED,
                TODAY.plusMonths(6));
        Inspection expired = inspection(2L, "HERM-TK-2026-01", InspectionCategory.HERMETICIDAD, EXECUTED,
                TODAY.minusDays(1));
        when(inspectionRepository.search(null, null, null)).thenReturn(List.of(valid, expired));

        List<Inspection> found = service.search(null, null, null, null);

        assertThat(found).extracting(Inspection::getStatus)
                .containsExactly(InspectionStatus.VIGENTE, InspectionStatus.VENCIDO);
    }

    @Test
    void searchRejectsAStartDateAfterTheEndDate() {
        assertThatThrownBy(() -> service.search(null, null, TODAY, TODAY.minusDays(1)))
                .isInstanceOf(InvalidInspectionException.class);
        verifyNoInteractions(inspectionRepository);
    }
}
