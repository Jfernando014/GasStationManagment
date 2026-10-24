package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.inventory.FuelPriceInfo;
import com.edu.unicauca.gasstation.backend.inventory.FuelPriceQuery;
import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.FuelPriceReadPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FuelPriceReadPortAdapter implements FuelPriceReadPort {

    private final Optional<FuelPriceQuery> fuelPriceQuery;

    @Override
    public Optional<BigDecimal> findEffectivePrice(String fuelType, LocalDate date) {
        if (fuelPriceQuery.isEmpty() || fuelType == null || date == null) {
            return Optional.empty();
        }
        try {
            FuelType type = FuelType.valueOf(fuelType.toUpperCase());
            return fuelPriceQuery.get().findEffectivePrice(type, date)
                    .map(FuelPriceInfo::getPricePerGallon);
        } catch (IllegalArgumentException e) {
            log.debug("Unknown fuel type: {}", fuelType);
            return Optional.empty();
        }
    }

    @Override
    public Map<String, BigDecimal> findEffectivePrices(LocalDate date) {
        if (fuelPriceQuery.isEmpty() || date == null) {
            return Collections.emptyMap();
        }
        Map<String, BigDecimal> prices = new HashMap<>();
        for (FuelType type : FuelType.values()) {
            try {
                fuelPriceQuery.get().findEffectivePrice(type, date)
                        .ifPresent(info -> prices.put(type.name(), info.getPricePerGallon()));
            } catch (Exception ignored) {
            }
        }
        return Collections.unmodifiableMap(prices);
    }
}
