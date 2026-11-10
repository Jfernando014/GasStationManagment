package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.inventory.InventoryClosingQuery;
import com.edu.unicauca.gasstation.backend.inventory.InventoryClosingSnapshot;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Adapts the public {@link InventoryClosingQuery} published by {@code inventory} to the Step 6
 * closing summary port (S3-19). It replaces the temporary empty fallback.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryClosingReadPortAdapter implements InventoryClosingReadPort {

    private final Optional<InventoryClosingQuery> inventoryClosingQuery;

    @Override
    public ClosingInventorySnapshot findClosingSnapshot(LocalDate journalDate) {
        if (inventoryClosingQuery.isEmpty() || journalDate == null) {
            return ClosingInventorySnapshot.empty();
        }
        try {
            InventoryClosingSnapshot snapshot = inventoryClosingQuery.get().findClosingSnapshot(journalDate);
            return new ClosingInventorySnapshot(
                    snapshot.hasMeasurement(),
                    snapshot.variations().stream().map(this::toVariation).toList(),
                    snapshot.alerts().stream().map(this::toAlert).toList()
            );
        } catch (RuntimeException ex) {
            log.error("Could not read the inventory closing snapshot for {}", journalDate, ex);
            return ClosingInventorySnapshot.empty();
        }
    }

    private FuelVariationSummary toVariation(InventoryClosingSnapshot.FuelVariationData data) {
        return new FuelVariationSummary(
                data.fuelType(),
                data.initialGallons(),
                data.entriesGallons(),
                data.soldGallons(),
                data.theoreticalGallons(),
                data.measuredGallons(),
                data.varianceGallons(),
                data.toleranceGallons(),
                data.outOfTolerance(),
                data.status()
        );
    }

    private InventoryAlertSummary toAlert(InventoryClosingSnapshot.AlertData data) {
        return new InventoryAlertSummary(
                data.id(),
                data.alertType(),
                data.fuelType(),
                data.level(),
                data.status(),
                data.autonomyDays(),
                data.openedOn()
        );
    }
}
