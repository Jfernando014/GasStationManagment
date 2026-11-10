package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import com.edu.unicauca.gasstation.backend.inventory.InspectionCategory;
import com.edu.unicauca.gasstation.backend.inventory.InspectionStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.edu.unicauca.gasstation.backend.inventory.InspectionTestData.inspection;
import static org.assertj.core.api.Assertions.assertThat;

class InspectionTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 5);
    private static final LocalDate EXECUTED = LocalDate.of(2026, 2, 5);

    @Test
    void isExpiredWhenTheDueDateHasPassed() {
        Inspection inspection = inspection(1L, "CAL-ONAC-2026-08", InspectionCategory.ONAC,
                EXECUTED, TODAY.minusDays(1));

        assertThat(inspection.statusOn(TODAY)).isEqualTo(InspectionStatus.VENCIDO);
    }

    @Test
    void isAboutToExpireOnTheDueDateAndInsideTheWarningWindow() {
        Inspection onDueDate = inspection(1L, "CAL-ONAC-2026-08", InspectionCategory.ONAC, EXECUTED, TODAY);
        Inspection lastWarningDay = inspection(2L, "CAL-ONAC-2026-09", InspectionCategory.ONAC, EXECUTED,
                TODAY.plusDays(Inspection.EXPIRING_SOON_DAYS));

        assertThat(onDueDate.statusOn(TODAY)).isEqualTo(InspectionStatus.PROXIMO);
        assertThat(lastWarningDay.statusOn(TODAY)).isEqualTo(InspectionStatus.PROXIMO);
    }

    @Test
    void isValidAfterTheWarningWindow() {
        Inspection inspection = inspection(1L, "CAL-ONAC-2026-08", InspectionCategory.ONAC, EXECUTED,
                TODAY.plusDays(Inspection.EXPIRING_SOON_DAYS + 1));

        assertThat(inspection.statusOn(TODAY)).isEqualTo(InspectionStatus.VIGENTE);
    }

    @Test
    void hasNoStatusWithoutADueDate() {
        Inspection inspection = inspection(1L, "CAL-ONAC-2026-08", InspectionCategory.ONAC, EXECUTED, null);

        assertThat(inspection.statusOn(TODAY)).isNull();
    }
}
