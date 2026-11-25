package com.edu.unicauca.gasstation.backend.sales.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.edu.unicauca.gasstation.backend.sales.CashBalanceQuery;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalFullWorkbookData;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalSnapshot;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalStatus;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.DayStaffReadPort;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.DayStaffReadPort.AssignedVendor;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort.ClosingInventorySnapshot;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalFileService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JournalFullWorkbookServiceTest {

    @Test
    void composesAllPortsForJournalDate() {
        LocalDate date = LocalDate.of(2026, 8, 5);
        JournalSnapshot snapshot = new JournalSnapshot(
                10L, date, 289L, JournalStatus.CLOSED, LocalDateTime.of(2026, 8, 5, 6, 0), List.of());

        JournalFileService fileService = mock(JournalFileService.class);
        DayStaffReadPort staffPort = mock(DayStaffReadPort.class);
        SellerCloseQuery sellerCloseQuery = mock(SellerCloseQuery.class);
        CashBalanceQuery cashBalanceQuery = mock(CashBalanceQuery.class);
        InventoryClosingReadPort inventoryPort = mock(InventoryClosingReadPort.class);

        when(fileService.getWorkbookData(10L)).thenReturn(snapshot);
        when(staffPort.findStaffForDate(date))
                .thenReturn(List.of(new AssignedVendor("1001", "Carlos P.", "TITULAR", "DIA6", 1, "V-01")));
        when(sellerCloseQuery.findVendorClosesByDate(date)).thenReturn(List.of());
        when(sellerCloseQuery.findDaySummary(date)).thenReturn(Optional.empty());
        when(cashBalanceQuery.findDailyCashBalance(date)).thenReturn(Optional.empty());
        when(inventoryPort.findClosingSnapshot(date)).thenReturn(ClosingInventorySnapshot.empty());

        JournalFullWorkbookService service = new JournalFullWorkbookService(
                fileService, staffPort, sellerCloseQuery, cashBalanceQuery, inventoryPort);

        JournalFullWorkbookData result = service.buildWorkbookData(10L);

        assertEquals(snapshot, result.journal());
        assertEquals(1, result.staff().size());
        assertEquals("Carlos P.", result.staff().getFirst().name());
        assertNull(result.daySummary());
        assertNull(result.cashBalance());
        assertTrue(result.inventory().variations().isEmpty());
    }
}
