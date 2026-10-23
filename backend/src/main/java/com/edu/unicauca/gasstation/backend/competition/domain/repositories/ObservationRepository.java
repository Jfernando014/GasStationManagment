package com.edu.unicauca.gasstation.backend.competition.domain.repositories;

import com.edu.unicauca.gasstation.backend.competition.domain.models.PriceObservation;
import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ObservationRepository {
    PriceObservation append(PriceObservation observation);
    Optional<PriceObservation> findById(long id);
    History findHistory(Long competitorId, FuelType fuelType, Instant from, Instant to, int page, int size);
    List<PriceObservation> findLatest(Long competitorId, FuelType fuelType, Instant asOf, boolean includeInactive);
    record History(List<PriceObservation> content, int page, int size, long totalElements) { }
}
