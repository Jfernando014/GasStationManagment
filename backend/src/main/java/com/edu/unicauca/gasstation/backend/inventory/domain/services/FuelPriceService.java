package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.FuelPriceInfo;
import com.edu.unicauca.gasstation.backend.inventory.FuelPriceQuery;
import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelPrice;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.FuelPriceRepository;
import com.edu.unicauca.gasstation.backend.inventory.exception.FuelPriceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class FuelPriceService implements FuelPriceQuery {

    private final FuelPriceRepository fuelPriceRepository;

    public FuelPriceService(FuelPriceRepository fuelPriceRepository) {
        this.fuelPriceRepository = fuelPriceRepository;
    }

    @Transactional
    public FuelPrice create(FuelPrice price) {
        return fuelPriceRepository.save(price);
    }

    @Transactional
    public FuelPrice update(Long id, FuelPrice changes) {
        FuelPrice price = fuelPriceRepository.findById(id)
                .orElseThrow(() -> new FuelPriceNotFoundException(id));
        price.setFuelType(changes.getFuelType());
        price.setPricePerGallon(changes.getPricePerGallon());
        price.setValidFrom(changes.getValidFrom());
        return fuelPriceRepository.save(price);
    }

    @Transactional(readOnly = true)
    public FuelPrice findEffective(FuelType fuelType, LocalDate date) {
        return findEffectiveEntity(fuelType, date)
                .orElseThrow(() -> new FuelPriceNotFoundException(fuelType, date));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FuelPriceInfo> findEffectivePrice(FuelType fuelType, LocalDate date) {
        Objects.requireNonNull(fuelType, "fuelType must not be null");
        Objects.requireNonNull(date, "date must not be null");

        return findEffectiveEntity(fuelType, date)
                .map(price -> new FuelPriceInfo(
                        price.getFuelType(),
                        price.getPricePerGallon(),
                        price.getValidFrom()));
    }

    @Transactional(readOnly = true)
    public List<FuelPrice> findHistory(FuelType fuelType) {
        if (fuelType == null) {
            return fuelPriceRepository.findHistory();
        }
        return fuelPriceRepository.findHistory(fuelType);
    }

    private Optional<FuelPrice> findEffectiveEntity(FuelType fuelType, LocalDate date) {
        return fuelPriceRepository.findEffective(fuelType, date);
    }
}