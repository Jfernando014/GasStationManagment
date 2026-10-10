package com.edu.unicauca.gasstation.backend.inventory.infrastructure.persistence;

import com.edu.unicauca.gasstation.backend.inventory.domain.models.FuelPrice;
import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


public interface FuelPriceRepository extends JpaRepository<FuelPrice, Long> {
    //Devuelve el precio vigente de un combustible en una fecha dada
    Optional<FuelPrice> findFirstByFuelTypeAndValidFromLessThanEqualOrderByValidFromDesc(
            FuelType fuelType, LocalDate date);

    //Devuelve el historial completo de precios de un combustible
    List<FuelPrice> findByFuelTypeOrderByValidFromDesc(FuelType fuelType);

}
