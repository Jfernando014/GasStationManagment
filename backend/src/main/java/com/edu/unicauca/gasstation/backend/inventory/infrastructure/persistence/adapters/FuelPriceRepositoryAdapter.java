package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelPrice;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.FuelPriceRepository;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers.FuelPricePersistenceMapper;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.repositories.JpaFuelPriceRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class FuelPriceRepositoryAdapter implements FuelPriceRepository {

    private final JpaFuelPriceRepository repository;
    private final FuelPricePersistenceMapper mapper;

    public FuelPriceRepositoryAdapter(
            JpaFuelPriceRepository repository,
            FuelPricePersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public FuelPrice save(FuelPrice price) {
        return mapper.toDomain(repository.save(mapper.toEntity(price)));
    }

    @Override
    public Optional<FuelPrice> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<FuelPrice> findEffective(FuelType fuelType, LocalDate date) {
        return repository
                .findFirstByFuelTypeAndValidFromLessThanEqualOrderByValidFromDesc(fuelType, date)
                .map(mapper::toDomain);
    }

    @Override
    public List<FuelPrice> findHistory(FuelType fuelType) {
        return repository.findByFuelTypeOrderByValidFromDesc(fuelType)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<FuelPrice> findHistory() {
        return repository.findAllByOrderByFuelTypeAscValidFromDesc()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
