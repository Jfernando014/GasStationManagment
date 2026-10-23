package com.edu.unicauca.gasstation.backend.competition.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.competition.domain.models.PriceObservation;
import com.edu.unicauca.gasstation.backend.competition.domain.repositories.ObservationRepository;
import com.edu.unicauca.gasstation.backend.competition.infrastructure.persistence.entities.ObservationEntity;
import com.edu.unicauca.gasstation.backend.competition.infrastructure.persistence.repositories.JpaObservationRepository;
import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ObservationRepositoryAdapter implements ObservationRepository {
    private final JpaObservationRepository observations;
    private final EntityManager entities;

    @Override @Transactional
    public PriceObservation append(PriceObservation o) {
        var entity = new ObservationEntity();
        entity.setCompetitorId(o.competitorId()); entity.setFuelType(o.fuelType());
        entity.setPricePerGallon(o.pricePerGallon()); entity.setObservedAt(o.observedAt());
        entity = observations.saveAndFlush(entity);
        entities.refresh(entity);
        return toDomain(entity);
    }
    @Override public Optional<PriceObservation> findById(long id) { return observations.findById(id).map(ObservationRepositoryAdapter::toDomain); }
    @Override public History findHistory(Long competitorId, FuelType fuelType, Instant from, Instant to, int page, int size) {
        var result = observations.findHistory(competitorId, fuelType, from, to, PageRequest.of(page, size));
        return new History(result.getContent().stream().map(ObservationRepositoryAdapter::toDomain).toList(), page, size, result.getTotalElements());
    }
    @Override public List<PriceObservation> findLatest(Long competitorId, FuelType fuelType, Instant asOf, boolean includeInactive) {
        return observations.findLatest(competitorId, fuelType == null ? null : fuelType.name(), asOf, includeInactive)
                .stream().map(ObservationRepositoryAdapter::toDomain).toList();
    }
    private static PriceObservation toDomain(ObservationEntity o) {
        return new PriceObservation(o.getId(), o.getCompetitorId(), o.getFuelType(), o.getPricePerGallon(), o.getObservedAt(), o.getCreatedAt());
    }
}
