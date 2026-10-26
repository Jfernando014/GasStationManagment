package com.edu.unicauca.gasstation.backend.inventory.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Measurement of the tanks of a journal. One per journal date, with one reading per tank. */
@Getter
@Setter
@NoArgsConstructor
public class TankMeasurement {

    private Long id;
    private LocalDate journalDate;
    private LocalDateTime measuredAt;
    private List<TankReading> readings = new ArrayList<>();

    public Optional<TankReading> readingOf(Long tankId) {
        return readings.stream().filter(r -> r.getTankId().equals(tankId)).findFirst();
    }
}