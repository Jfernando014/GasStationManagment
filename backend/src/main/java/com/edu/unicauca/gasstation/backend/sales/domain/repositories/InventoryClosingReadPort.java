package com.edu.unicauca.gasstation.backend.sales.domain.repositories;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Secondary port for consuming fuel variations and new inventory alerts in Step 6 closing summary (S3-05).
 * Decouples the {@code sales} module from internal {@code inventory} entities while consuming {@code inventoryQuery}.
 */
public interface InventoryClosingReadPort {

    /**
     * Retrieves the fuel variations and new autonomy alerts generated on the given operational journal date.
     */
    ClosingInventorySnapshot findClosingSnapshot(LocalDate journalDate);

    record ClosingInventorySnapshot(
            boolean hasMeasurement,
            List<FuelVariationSummary> variations,
            List<InventoryAlertSummary> newAlerts
    ) {
        public ClosingInventorySnapshot {
            variations = variations != null ? List.copyOf(variations) : List.of();
            newAlerts = newAlerts != null ? List.copyOf(newAlerts) : List.of();
        }

        public static ClosingInventorySnapshot empty() {
            return new ClosingInventorySnapshot(false, List.of(), List.of());
        }
    }

    record FuelVariationSummary(
            String fuelType,
            BigDecimal initialGallons,
            BigDecimal entriesGallons,
            BigDecimal soldGallons,
            BigDecimal theoreticalGallons,
            BigDecimal measuredGallons,
            BigDecimal varianceGallons,
            BigDecimal toleranceGallons,
            boolean outOfTolerance,
            String status
    ) {}

    record InventoryAlertSummary(
            Long id,
            String alertType,
            String fuelType,
            String level,
            String status,
            BigDecimal autonomyDays,
            LocalDate openedOn
    ) {}
}
