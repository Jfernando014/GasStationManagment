package com.edu.unicauca.gasstation.backend.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Inventory state of an operational journal date, published for other modules (S3-19).
 *
 * <p>It is intentionally a public value object: the {@code sales} module maps it into its own
 * {@code InventoryClosingReadPort} records, so {@code inventory} never depends on {@code sales}.
 *
 * @param hasMeasurement whether a tank measurement exists for the journal date
 * @param variations     variation of each fuel on the journal date
 * @param alerts         alerts opened on the journal date
 */
public record InventoryClosingSnapshot(
        boolean hasMeasurement,
        List<FuelVariationData> variations,
        List<AlertData> alerts
) {

    public InventoryClosingSnapshot {
        variations = variations != null ? List.copyOf(variations) : List.of();
        alerts = alerts != null ? List.copyOf(alerts) : List.of();
    }

    /**
     * Variation of a fuel on the journal date.
     */
    public record FuelVariationData(
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

    /**
     * Alert opened on the journal date.
     */
    public record AlertData(
            Long id,
            String alertType,
            String fuelType,
            String level,
            String status,
            BigDecimal autonomyDays,
            LocalDate openedOn
    ) {}
}
