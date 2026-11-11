package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import com.edu.unicauca.gasstation.backend.inventory.InspectionCategory;
import com.edu.unicauca.gasstation.backend.inventory.InspectionStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class InspectionResponse {

    private Long id;
    private String code;
    private InspectionCategory category;
    private String target;
    private LocalDate dateExecuted;
    private LocalDate dateDue;
    private String certifier;
    private String toleranceMeasured;
    /** Computed by the backend from the due date; it is never sent back. */
    private InspectionStatus status;
    private String certDoc;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
