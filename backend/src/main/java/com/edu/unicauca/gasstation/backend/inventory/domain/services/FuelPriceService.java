package com.edu.unicauca.gasstation.backend.inventory.domain.services;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelPrice;
import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.exception.FuelPriceNotFoundException;
import com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence.FuelPriceRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class FuelPriceService {

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
        return fuelPriceRepository.saveAndFlush(price);
    }

    @Transactional(readOnly = true)
    public FuelPrice findEffective(FuelType fuelType, LocalDate date) {
        return fuelPriceRepository
                .findFirstByFuelTypeAndValidFromLessThanEqualOrderByValidFromDesc(fuelType, date)
                .orElseThrow(() -> new FuelPriceNotFoundException(fuelType, date));
    }

    @Transactional(readOnly = true)
    public List<FuelPrice> findHistory(FuelType fuelType) {
        if (fuelType == null) {
            return fuelPriceRepository.findAll(Sort.by(Sort.Order.asc("fuelType"), Sort.Order.desc("validFrom")));
        }
        return fuelPriceRepository.findByFuelTypeOrderByValidFromDesc(fuelType);
    }
}