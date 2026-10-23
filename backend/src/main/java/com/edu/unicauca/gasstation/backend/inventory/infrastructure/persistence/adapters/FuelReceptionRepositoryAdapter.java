package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelReception;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.FuelReceptionRepository;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers.FuelReceptionPersistenceMapper;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.repositories.JpaFuelReceptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FuelReceptionRepositoryAdapter implements FuelReceptionRepository {

    private final JpaFuelReceptionRepository repository;
    private final FuelReceptionPersistenceMapper mapper;

    @Override
    public FuelReception save(FuelReception reception) {
        return mapper.toDomain(repository.save(mapper.toEntity(reception)));
    }

    @Override
    public Optional<FuelReception> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<FuelReception> search(Long tankId, LocalDate from, LocalDate to) {
        return repository.search(tankId, from, to).stream().map(mapper::toDomain).toList();
    }

    @Override
    public BigDecimal sumGallonsAfter(Long tankId, LocalDate date, Long excludedReceptionId) {
        BigDecimal sum = repository.sumGallonsAfter(tankId, date, excludedReceptionId);
        return sum == null ? BigDecimal.ZERO : sum;
    }
}