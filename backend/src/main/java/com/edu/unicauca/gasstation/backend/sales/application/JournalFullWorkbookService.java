package com.edu.unicauca.gasstation.backend.sales.application;

import com.edu.unicauca.gasstation.backend.sales.CashBalanceQuery;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.DaySummary;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.VendorCloseSummary;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalFullWorkbookData;
import com.edu.unicauca.gasstation.backend.sales.domain.JournalSnapshot;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.DayStaffReadPort;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.DayStaffReadPort.AssignedVendor;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort.ClosingInventorySnapshot;
import com.edu.unicauca.gasstation.backend.sales.domain.services.JournalFileService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aggregates the 6 operational steps of a journal so the full workbook (.xlsx) can be exported
 * directly without navigating step by step.
 */
@Service
public class JournalFullWorkbookService {

    private final JournalFileService journalFileService;
    private final DayStaffReadPort dayStaffReadPort;
    private final SellerCloseQuery sellerCloseQuery;
    private final CashBalanceQuery cashBalanceQuery;
    private final InventoryClosingReadPort inventoryClosingReadPort;

    public JournalFullWorkbookService(
            JournalFileService journalFileService,
            DayStaffReadPort dayStaffReadPort,
            SellerCloseQuery sellerCloseQuery,
            CashBalanceQuery cashBalanceQuery,
            InventoryClosingReadPort inventoryClosingReadPort) {
        this.journalFileService = journalFileService;
        this.dayStaffReadPort = dayStaffReadPort;
        this.sellerCloseQuery = sellerCloseQuery;
        this.cashBalanceQuery = cashBalanceQuery;
        this.inventoryClosingReadPort = inventoryClosingReadPort;
    }

    @Transactional(readOnly = true)
    public JournalFullWorkbookData buildWorkbookData(Long journalId) {
        JournalSnapshot journal = journalFileService.getWorkbookData(journalId);
        LocalDate date = journal.date();

        List<AssignedVendor> staff = safeList(dayStaffReadPort.findStaffForDate(date));
        List<VendorCloseSummary> vendorCloses = safeList(sellerCloseQuery.findVendorClosesByDate(date));
        DaySummary daySummary = sellerCloseQuery.findDaySummary(date).orElse(null);
        CashBalanceTableInfo cashBalance = cashBalanceQuery.findDailyCashBalance(date).orElse(null);
        ClosingInventorySnapshot inventory = inventoryClosingReadPort.findClosingSnapshot(date);

        return new JournalFullWorkbookData(
                journal,
                staff,
                daySummary,
                vendorCloses,
                cashBalance,
                inventory != null ? inventory : ClosingInventorySnapshot.empty());
    }

    private static <T> List<T> safeList(List<T> values) {
        return values != null ? values : List.of();
    }
}
