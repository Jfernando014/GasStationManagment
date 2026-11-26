package com.edu.unicauca.gasstation.backend.inventory.infrastructure.targets;

import com.edu.unicauca.gasstation.backend.inventory.FuelMonthlyTargetPort;
import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Optional;

/**
 * TEMPORARY implementation while Metas does not provide the fuel targets: no fuel has one. Delete this class when
 * the real implementation of {@link FuelMonthlyTargetPort} is added.
 */
@Component
public class TemporaryFuelMonthlyTargetAdapter implements FuelMonthlyTargetPort {

    @Override
    public Optional<BigDecimal> findTargetGallons(FuelType fuelType, YearMonth month) {
        return Optional.empty();
    }
}
