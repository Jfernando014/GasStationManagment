package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import com.edu.unicauca.gasstation.backend.inventory.InspectionCategory;
import com.edu.unicauca.gasstation.backend.inventory.InspectionStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
@Setter
@NoArgsConstructor
public class Inspection {

    /** An inspection due within this many days is considered about to expire. */
    public static final int EXPIRING_SOON_DAYS = 60;

    private Long id;
    private String code;
    private InspectionCategory category;
    private String target;
    private LocalDate dateExecuted;
    private LocalDate dateDue;
    private String certifier;
    private String toleranceMeasured;
    private String certDoc;
    /** Derived from {@link #dateDue}; it is not persisted. */
    private InspectionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Regulatory status on the given date: expired when the due date has passed, about to expire
     * during the warning window and valid afterwards.
     */
    public InspectionStatus statusOn(LocalDate today) {
        if (dateDue == null) {
            return null;
        }
        if (dateDue.isBefore(today)) {
            return InspectionStatus.VENCIDO;
        }
        if (ChronoUnit.DAYS.between(today, dateDue) <= EXPIRING_SOON_DAYS) {
            return InspectionStatus.PROXIMO;
        }
        return InspectionStatus.VIGENTE;
    }
}
