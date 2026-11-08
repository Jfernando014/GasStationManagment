package com.edu.unicauca.gasstation.backend.sales.api.dto;

import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalResponse {
    private Long id;
    private LocalDate date;
    private Long consecutive;
    private JournalStatus status;
    private LocalDateTime closedAt;
    private List<JournalStepResponse> steps;
}
