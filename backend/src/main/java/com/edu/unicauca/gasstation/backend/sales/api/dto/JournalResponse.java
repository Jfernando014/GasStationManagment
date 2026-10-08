package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class JournalResponse {
    private Long id;
    private LocalDate date;
    private Long consecutive;
    private JournalStatus status;
    private List<JournalStepResponse> steps;
}
