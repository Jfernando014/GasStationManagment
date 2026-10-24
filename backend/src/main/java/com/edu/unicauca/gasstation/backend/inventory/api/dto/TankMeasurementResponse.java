package com.edu.unicauca.gasstation.backend.inventory.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class TankMeasurementResponse {

    private Long id;
    private LocalDate journalDate;
    private LocalDateTime measuredAt;
    private List<TankReadingResponse> readings;
}