package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.inventory.InventoryClosingQuery;
import com.edu.unicauca.gasstation.backend.inventory.InventoryClosingSnapshot;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryClosingReadPortAdapterTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);

    @Test
    void mapsTheInventorySnapshotToTheSalesPort() {
        InventoryClosingQuery query = journalDate -> new InventoryClosingSnapshot(
                true,
                List.of(new InventoryClosingSnapshot.FuelVariationData(
                        "MOTOR", new BigDecimal("1558.05"), new BigDecimal("3970"), new BigDecimal("784.587"),
                        new BigDecimal("4743.463"), new BigDecimal("4703.60"), new BigDecimal("-39.863"),
                        new BigDecimal("30"), true, "OK")),
                List.of(new InventoryClosingSnapshot.AlertData(
                        7L, "AUTONOMY", "MOTOR", "CRITICAL", "ACTIVE", new BigDecimal("1.25"), DATE))
        );
        InventoryClosingReadPortAdapter adapter = new InventoryClosingReadPortAdapter(Optional.of(query));

        InventoryClosingReadPort.ClosingInventorySnapshot snapshot = adapter.findClosingSnapshot(DATE);

        assertThat(snapshot.hasMeasurement()).isTrue();
        assertThat(snapshot.variations()).hasSize(1);
        assertThat(snapshot.variations().get(0).fuelType()).isEqualTo("MOTOR");
        assertThat(snapshot.variations().get(0).varianceGallons()).isEqualByComparingTo("-39.863");
        assertThat(snapshot.variations().get(0).outOfTolerance()).isTrue();
        assertThat(snapshot.newAlerts()).hasSize(1);
        assertThat(snapshot.newAlerts().get(0).id()).isEqualTo(7L);
        assertThat(snapshot.newAlerts().get(0).level()).isEqualTo("CRITICAL");
        assertThat(snapshot.newAlerts().get(0).openedOn()).isEqualTo(DATE);
    }

    @Test
    void returnsTheEmptySnapshotWhenTheQueryIsMissing() {
        InventoryClosingReadPortAdapter adapter = new InventoryClosingReadPortAdapter(Optional.empty());

        InventoryClosingReadPort.ClosingInventorySnapshot snapshot = adapter.findClosingSnapshot(DATE);

        assertThat(snapshot.hasMeasurement()).isFalse();
        assertThat(snapshot.variations()).isEmpty();
        assertThat(snapshot.newAlerts()).isEmpty();
    }

    @Test
    void returnsTheEmptySnapshotWhenTheDateIsMissing() {
        InventoryClosingQuery query = journalDate -> {
            throw new AssertionError("The query must not be called without a date");
        };
        InventoryClosingReadPortAdapter adapter = new InventoryClosingReadPortAdapter(Optional.of(query));

        assertThat(adapter.findClosingSnapshot(null).hasMeasurement()).isFalse();
    }

    @Test
    void returnsTheEmptySnapshotWhenInventoryFails() {
        InventoryClosingQuery query = journalDate -> {
            throw new IllegalStateException("boom");
        };
        InventoryClosingReadPortAdapter adapter = new InventoryClosingReadPortAdapter(Optional.of(query));

        assertThat(adapter.findClosingSnapshot(DATE).hasMeasurement()).isFalse();
    }
}
