package com.edu.unicauca.gasstation.backend.inventory.domain.repositories;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelReception;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FuelReceptionRepository {

    FuelReception save(FuelReception reception);

    Optional<FuelReception> findById(Long id);

    List<FuelReception> search(Long tankId, LocalDate from, LocalDate to);


    BigDecimal sumGallonsAfter(Long tankId, LocalDate date, Long excludedReceptionId);

    /**
     * Gallons received after a measurement: the receptions dated after the measurement date plus the ones dated the
     * same day that were registered after the measurement was taken.
     */
    BigDecimal sumGallonsAfterMeasurement(Long tankId, LocalDate measurementDate, LocalDateTime measuredAt);
}