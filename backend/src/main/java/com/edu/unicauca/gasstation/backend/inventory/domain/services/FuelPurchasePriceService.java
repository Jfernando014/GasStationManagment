package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.FuelPurchasePriceInfo;
import com.edu.unicauca.gasstation.backend.inventory.FuelPurchasePriceQuery;
import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelPurchasePrice;
import com.edu.unicauca.gasstation.backend.inventory.domain.repositories.FuelPurchasePriceRepository;
import com.edu.unicauca.gasstation.backend.inventory.exception.FuelPurchasePriceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class FuelPurchasePriceService implements FuelPurchasePriceQuery {

    private final FuelPurchasePriceRepository fuelPurchasePriceRepository;

    public FuelPurchasePriceService(FuelPurchasePriceRepository fuelPurchasePriceRepository) {
        this.fuelPurchasePriceRepository = fuelPurchasePriceRepository;
    }

    @Transactional
    public FuelPurchasePrice create(FuelPurchasePrice price) {
        return fuelPurchasePriceRepository.save(price);
    }

    @Transactional
    public FuelPurchasePrice update(Long id, FuelPurchasePrice changes) {
        FuelPurchasePrice price = fuelPurchasePriceRepository.findById(id)
                .orElseThrow(() -> new FuelPurchasePriceNotFoundException(id));
        price.setFuelType(changes.getFuelType());
        price.setPricePerGallon(changes.getPricePerGallon());
        price.setValidFrom(changes.getValidFrom());
        return fuelPurchasePriceRepository.save(price);
    }

    @Transactional(readOnly = true)
    public FuelPurchasePrice findEffective(FuelType fuelType, LocalDate date) {
        return findEffectiveEntity(fuelType, date)
                .orElseThrow(() -> new FuelPurchasePriceNotFoundException(fuelType, date));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FuelPurchasePriceInfo> findEffectivePrice(FuelType fuelType, LocalDate date) {
        Objects.requireNonNull(fuelType, "fuelType must not be null");
        Objects.requireNonNull(date, "date must not be null");

        return findEffectiveEntity(fuelType, date)
                .map(price -> new FuelPurchasePriceInfo(
                        price.getFuelType(),
                        price.getPricePerGallon(),
                        price.getValidFrom()));
    }

    @Transactional(readOnly = true)
    public List<FuelPurchasePrice> findHistory(FuelType fuelType) {
        if (fuelType == null) {
            return fuelPurchasePriceRepository.findHistory();
        }
        return fuelPurchasePriceRepository.findHistory(fuelType);
    }

    private Optional<FuelPurchasePrice> findEffectiveEntity(FuelType fuelType, LocalDate date) {
        return fuelPurchasePriceRepository.findEffective(fuelType, date);
    }
}
