package com.edu.unicauca.gasstation.backend.sales.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.edu.unicauca.gasstation.backend.sales.MigratedHistoryQuery.MigratedDay;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Tests the migrated August history public query.
 *
 * @author javiersolanop777
 */
class MigratedHistoryQueryServiceTest {

    private final MigratedHistoryQueryService service = new MigratedHistoryQueryService();

    @Test
    void publishesTheAugustSnapshotWithVendorsVariationsAndCash() {
        MigratedDay day = service.findDay(LocalDate.of(2026, 8, 1)).orElseThrow();

        assertEquals(LocalDate.of(2026, 8, 1), day.date());
        assertEquals(3, day.vendors().size());
        assertEquals("1001", day.vendors().get(0).vendorDocument());
        assertEquals(2, day.variations().size());
        assertEquals("MOTOR", day.variations().get(0).fuelType());
        assertEquals(289L, day.cash().planilla());
        assertEquals(new java.math.BigDecimal("35000.00"), day.electronic().points());
    }

    @Test
    void doesNotPublishTheLiveDemoDate() {
        assertTrue(service.findDay(LocalDate.of(2026, 8, 5)).isEmpty());
    }

    @Test
    void doesNotPublishDatesOutsideTheMigratedWindow() {
        assertTrue(service.findDay(LocalDate.of(2026, 7, 31)).isEmpty());
        assertTrue(service.findDay(LocalDate.of(2026, 8, 29)).isEmpty());
    }

    @Test
    void nullDateReturnsEmpty() {
        assertTrue(service.findDay(null).isEmpty());
    }
}
