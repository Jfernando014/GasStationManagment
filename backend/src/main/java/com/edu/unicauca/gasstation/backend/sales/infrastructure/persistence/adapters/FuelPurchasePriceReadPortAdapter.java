package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.inventory.FuelPurchasePriceInfo;
import com.edu.unicauca.gasstation.backend.inventory.FuelPurchasePriceQuery;
import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.FuelPurchasePriceReadPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FuelPurchasePriceReadPortAdapter implements FuelPurchasePriceReadPort {

    private final Optional<FuelPurchasePriceQuery> fuelPurchasePriceQuery;

    @Override
    public Optional<BigDecimal> findEffectivePrice(String fuelType, LocalDate date) {
        if (fuelPurchasePriceQuery.isEmpty() || fuelType == null || date == null) {
            return Optional.empty();
        }
        try {
            FuelType type = FuelType.valueOf(fuelType.toUpperCase());
            return fuelPurchasePriceQuery.get().findEffectivePrice(type, date)
                    .map(FuelPurchasePriceInfo::getPricePerGallon);
        } catch (IllegalArgumentException e) {
            log.debug("Unknown fuel type: {}", fuelType);
            return Optional.empty();
        }
    }
}
