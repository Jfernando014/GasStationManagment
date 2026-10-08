package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.StepStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class JournalStepResponse {
    private Long id;
    private Integer stepNumber;
    private String name;
    private StepStatus status;
    private LocalDateTime completedAt;
}
