package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.sales.domain.models.DiscrepancyToleranceConfig;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.DiscrepancyToleranceRepository;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.entities.SalesDiscrepancyToleranceEntity;
import com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.repositories.SalesDiscrepancyToleranceJpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DiscrepancyToleranceRepositoryAdapter implements DiscrepancyToleranceRepository {

    private final SalesDiscrepancyToleranceJpaRepository jpaRepository;

    @Override
    public Optional<DiscrepancyToleranceConfig> findEffectiveTolerance(LocalDate date) {
        return jpaRepository.findFirstByEffectiveDateLessThanEqualOrderByEffectiveDateDesc(date)
                .map(this::toDomain);
    }

    @Override
    public Optional<DiscrepancyToleranceConfig> findByEffectiveDate(LocalDate effectiveDate) {
        return jpaRepository.findByEffectiveDate(effectiveDate)
                .map(this::toDomain);
    }

    @Override
    public DiscrepancyToleranceConfig save(DiscrepancyToleranceConfig config) {
        SalesDiscrepancyToleranceEntity entity = toEntity(config);
        SalesDiscrepancyToleranceEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<DiscrepancyToleranceConfig> findAllHistory() {
        return jpaRepository.findAllByOrderByEffectiveDateDesc().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<DiscrepancyToleranceConfig> findScheduledAfter(LocalDate date) {
        return jpaRepository.findAllByEffectiveDateGreaterThanOrderByEffectiveDateAsc(date).stream()
                .map(this::toDomain)
                .toList();
    }

    private DiscrepancyToleranceConfig toDomain(SalesDiscrepancyToleranceEntity entity) {
        return new DiscrepancyToleranceConfig(
                entity.getId(),
                entity.getToleranceAmount(),
                entity.getEffectiveDate(),
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getCreatedBy()
        );
    }

    private SalesDiscrepancyToleranceEntity toEntity(DiscrepancyToleranceConfig domain) {
        return SalesDiscrepancyToleranceEntity.builder()
                .id(domain.id())
                .toleranceAmount(domain.toleranceAmount())
                .effectiveDate(domain.effectiveDate())
                .notes(domain.notes())
                .createdAt(domain.createdAt())
                .createdBy(domain.createdBy())
                .build();
    }
}
