package com.edu.unicauca.gasstation.backend.sales.domain;

import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.DaySummary;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.VendorCloseSummary;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.DayStaffReadPort.AssignedVendor;
import com.edu.unicauca.gasstation.backend.sales.domain.repositories.InventoryClosingReadPort.ClosingInventorySnapshot;
import java.util.List;
import java.util.Objects;

/**
 * Consolidated domain data for exporting a complete journal workbook (.xlsx) covering all 6 steps.
 */
public record JournalFullWorkbookData(
        JournalSnapshot journal,
        List<AssignedVendor> staff,
        DaySummary daySummary,
        List<VendorCloseSummary> vendorCloses,
        CashBalanceTableInfo cashBalance,
        ClosingInventorySnapshot inventory) {

    public JournalFullWorkbookData {
        Objects.requireNonNull(journal, "journal");
        staff = staff != null ? List.copyOf(staff) : List.of();
        vendorCloses = vendorCloses != null ? List.copyOf(vendorCloses) : List.of();
        inventory = inventory != null ? inventory : ClosingInventorySnapshot.empty();
    }

    public static JournalFullWorkbookData fromSnapshot(JournalSnapshot journal) {
        return new JournalFullWorkbookData(
                journal,
                List.of(),
                null,
                List.of(),
                null,
                ClosingInventorySnapshot.empty());
    }
}
