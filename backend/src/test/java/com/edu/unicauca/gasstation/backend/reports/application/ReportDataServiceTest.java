package com.edu.unicauca.gasstation.backend.reports.application;

import static com.edu.unicauca.gasstation.backend.reports.ReportTestData.DATE;
import static com.edu.unicauca.gasstation.backend.reports.ReportTestData.NEXT_DATE;
import static com.edu.unicauca.gasstation.backend.reports.ReportTestData.bd;
import static com.edu.unicauca.gasstation.backend.reports.ReportTestData.cashTable;
import static com.edu.unicauca.gasstation.backend.reports.ReportTestData.daySummary;
import static com.edu.unicauca.gasstation.backend.reports.ReportTestData.migratedQueryDay;
import static com.edu.unicauca.gasstation.backend.reports.ReportTestData.vendorClose;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.inventory.InventoryClosingSnapshot;
import com.edu.unicauca.gasstation.backend.reports.domain.models.ReportDay;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests the live and migrated data merge of the reports module.
 *
 * @author javiersolanop777
 */
class ReportDataServiceTest {

    @Test
    void prefersLiveDataAndUsesMigratedOnlyAsSourceFallback() {
        SellerCloseQuery sellerCloseQuery = mock(SellerCloseQuery.class);
        when(sellerCloseQuery.findDaySummary(DATE)).thenReturn(Optional.of(daySummary()));
        when(sellerCloseQuery.findVendorClosesByDate(DATE)).thenReturn(List.of(vendorClose()));

        ReportDataService service = new ReportDataService(
                sellerCloseQuery,
                date -> new InventoryClosingSnapshot(false, List.of(), List.of()),
                date -> Optional.empty(),
                date -> Optional.of(migratedQueryDay()));

        ReportDay day = service.findDay(DATE).orElseThrow();

        assertEquals(ReportDay.Source.LIVE, day.source());
        assertEquals(bd("1000"), day.totalGallons());
        assertEquals(bd("1200000"), day.totalDelivered());
        assertEquals(bd("900000"), day.totalCash());
        assertEquals(bd("57000"), day.totalBasket());
        assertEquals(bd("-531"), day.totalDiscrepancy());
        assertEquals(1, day.vendors().size());
        assertEquals(1, day.variations().size());
        assertEquals("DIESEL", day.variations().get(0).fuelType());
        assertEquals(bd("6910000"), day.cash().previousCash());
    }

    @Test
    void usesMigratedDayWhenLiveSourcesPublishNothing() {
        SellerCloseQuery sellerCloseQuery = mock(SellerCloseQuery.class);
        when(sellerCloseQuery.findDaySummary(NEXT_DATE)).thenReturn(Optional.empty());
        when(sellerCloseQuery.findVendorClosesByDate(NEXT_DATE)).thenReturn(List.of());

        ReportDataService service = new ReportDataService(
                sellerCloseQuery,
                date -> new InventoryClosingSnapshot(false, List.of(), List.of()),
                date -> Optional.empty(),
                date -> Optional.of(migratedQueryDay()));

        ReportDay day = service.findDay(NEXT_DATE).orElseThrow();

        assertEquals(ReportDay.Source.MIGRATED, day.source());
        assertEquals(bd("245"), day.totalGallons());
        assertEquals(bd("4120000"), day.totalDelivered());
        assertNull(day.totalCash());
        assertEquals(1, day.variations().size());
        assertEquals("DIESEL", day.fuelSales().get(0).fuelType());
        assertEquals(290L, day.cash().planilla());
    }

    @Test
    void liveCashWinsOverMigratedCash() {
        SellerCloseQuery sellerCloseQuery = mock(SellerCloseQuery.class);
        when(sellerCloseQuery.findDaySummary(DATE)).thenReturn(Optional.of(daySummary()));
        when(sellerCloseQuery.findVendorClosesByDate(DATE)).thenReturn(List.of(vendorClose()));

        ReportDataService service = new ReportDataService(
                sellerCloseQuery,
                date -> new InventoryClosingSnapshot(false, List.of(), List.of()),
                date -> Optional.of(cashTable()),
                date -> Optional.of(migratedQueryDay()));

        ReportDay day = service.findDay(DATE).orElseThrow();

        assertEquals(289L, day.cash().planilla());
        assertEquals("CLOSED", day.cash().journalStatus());
        assertEquals(bd("7900000"), day.cash().planillaCashBalance());
    }

    @Test
    void returnsEmptyWhenNeitherLiveNorMigratedDataExists() {
        SellerCloseQuery sellerCloseQuery = mock(SellerCloseQuery.class);
        when(sellerCloseQuery.findDaySummary(DATE)).thenReturn(Optional.empty());
        when(sellerCloseQuery.findVendorClosesByDate(DATE)).thenReturn(List.of());

        ReportDataService service = new ReportDataService(
                sellerCloseQuery,
                date -> new InventoryClosingSnapshot(false, List.of(), List.of()),
                date -> Optional.empty(),
                date -> Optional.empty());

        assertTrue(service.findDay(DATE).isEmpty());
    }

    @Test
    void findDaysSkipsDatesWithoutInformation() {
        SellerCloseQuery sellerCloseQuery = mock(SellerCloseQuery.class);
        when(sellerCloseQuery.findDaySummary(DATE)).thenReturn(Optional.empty());
        when(sellerCloseQuery.findVendorClosesByDate(DATE)).thenReturn(List.of());
        when(sellerCloseQuery.findDaySummary(NEXT_DATE)).thenReturn(Optional.empty());
        when(sellerCloseQuery.findVendorClosesByDate(NEXT_DATE)).thenReturn(List.of());

        ReportDataService service = new ReportDataService(
                sellerCloseQuery,
                date -> new InventoryClosingSnapshot(false, List.of(), List.of()),
                date -> Optional.empty(),
                date -> NEXT_DATE.equals(date) ? Optional.of(migratedQueryDay()) : Optional.empty());

        assertEquals(1, service.findDays(DATE, NEXT_DATE).size());
    }

    @Test
    void ignoresIncompleteInventoryVariationsWhenNoLiveMeasurementExists() {
        SellerCloseQuery sellerCloseQuery = mock(SellerCloseQuery.class);
        when(sellerCloseQuery.findDaySummary(DATE)).thenReturn(Optional.empty());
        when(sellerCloseQuery.findVendorClosesByDate(DATE)).thenReturn(List.of());

        InventoryClosingSnapshot incompleteSnapshot = new InventoryClosingSnapshot(
                false,
                List.of(incompleteVariation("MOTOR"), incompleteVariation("DIESEL")),
                List.of());

        ReportDataService service = new ReportDataService(
                sellerCloseQuery,
                date -> incompleteSnapshot,
                date -> Optional.empty(),
                date -> Optional.empty());

        assertTrue(service.findDay(DATE).isEmpty());
    }

    @Test
    void fallsBackToMigratedDayWhenInventoryHasOnlyIncompleteStubsWithoutMeasurement() {
        SellerCloseQuery sellerCloseQuery = mock(SellerCloseQuery.class);
        when(sellerCloseQuery.findDaySummary(NEXT_DATE)).thenReturn(Optional.empty());
        when(sellerCloseQuery.findVendorClosesByDate(NEXT_DATE)).thenReturn(List.of());

        InventoryClosingSnapshot incompleteSnapshot = new InventoryClosingSnapshot(
                false,
                List.of(incompleteVariation("MOTOR")),
                List.of());

        ReportDataService service = new ReportDataService(
                sellerCloseQuery,
                date -> incompleteSnapshot,
                date -> Optional.empty(),
                date -> Optional.of(migratedQueryDay()));

        ReportDay day = service.findDay(NEXT_DATE).orElseThrow();

        assertEquals(ReportDay.Source.MIGRATED, day.source());
        assertEquals(1, day.vendors().size());
        assertEquals(1, day.variations().size());
        assertEquals("DIESEL", day.variations().get(0).fuelType());
        assertEquals("OK", day.variations().get(0).status());
    }

    @Test
    void usesMigratedVariationsOnLiveDayWhenTankMeasurementHasNotBeenRecorded() {
        SellerCloseQuery sellerCloseQuery = mock(SellerCloseQuery.class);
        when(sellerCloseQuery.findDaySummary(DATE)).thenReturn(Optional.of(daySummary()));
        when(sellerCloseQuery.findVendorClosesByDate(DATE)).thenReturn(List.of(vendorClose()));

        InventoryClosingSnapshot incompleteSnapshot = new InventoryClosingSnapshot(
                false,
                List.of(incompleteVariation("MOTOR")),
                List.of());

        ReportDataService service = new ReportDataService(
                sellerCloseQuery,
                date -> incompleteSnapshot,
                date -> Optional.empty(),
                date -> Optional.of(migratedQueryDay()));

        ReportDay day = service.findDay(DATE).orElseThrow();

        assertEquals(ReportDay.Source.LIVE, day.source());
        assertEquals(1, day.variations().size());
        assertEquals("DIESEL", day.variations().get(0).fuelType());
        assertEquals("OK", day.variations().get(0).status());
    }

    private static InventoryClosingSnapshot.FuelVariationData incompleteVariation(String fuelType) {
        return new InventoryClosingSnapshot.FuelVariationData(
                fuelType, null, bd("0"), null, null, null, null, null, false, "INCOMPLETE_MEASUREMENT");
    }
}
