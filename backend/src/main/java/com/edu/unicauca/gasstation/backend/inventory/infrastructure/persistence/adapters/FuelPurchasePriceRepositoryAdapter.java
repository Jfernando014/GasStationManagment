package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelPurchasePrice;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.FuelPurchasePriceRepository;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.mappers.FuelPurchasePricePersistenceMapper;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.repositories.JpaFuelPurchasePriceRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class FuelPurchasePriceRepositoryAdapter implements FuelPurchasePriceRepository {

    private final JpaFuelPurchasePriceRepository repository;
    private final FuelPurchasePricePersistenceMapper mapper;

    public FuelPurchasePriceRepositoryAdapter(
            JpaFuelPurchasePriceRepository repository,
            FuelPurchasePricePersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public FuelPurchasePrice save(FuelPurchasePrice price) {
        return mapper.toDomain(repository.save(mapper.toEntity(price)));
    }

    @Override
    public Optional<FuelPurchasePrice> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<FuelPurchasePrice> findEffective(FuelType fuelType, LocalDate date) {
        return repository
                .findFirstByFuelTypeAndValidFromLessThanEqualOrderByValidFromDesc(fuelType, date)
                .map(mapper::toDomain);
    }

    @Override
    public List<FuelPurchasePrice> findHistory(FuelType fuelType) {
        return repository.findByFuelTypeOrderByValidFromDesc(fuelType)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<FuelPurchasePrice> findHistory() {
        return repository.findAllByOrderByFuelTypeAscValidFromDesc()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
