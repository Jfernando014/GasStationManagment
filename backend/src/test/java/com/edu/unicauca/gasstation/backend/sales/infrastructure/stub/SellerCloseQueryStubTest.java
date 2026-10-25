package com.edu.unicauca.gasstation.backend.sales.infrastructure.stub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.DaySummary;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.VendorCloseSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Checks that the stub data is consistent: the day totals must match the sum of the vendors.
 *
 * @author javiersolanop777
 */
class SellerCloseQueryStubTest {

    private final SellerCloseQueryStub atrStub = new SellerCloseQueryStub();

    @Test
    void dayTotalsMatchTheSumOfTheVendors() {
        LocalDate varDate = LocalDate.of(2026, 10, 24);
        DaySummary objDay = atrStub.findDaySummary(varDate).orElseThrow();
        List<VendorCloseSummary> objVendors = atrStub.findVendorClosesByDate(varDate);
        BigDecimal objCash = objVendors.stream().map(VendorCloseSummary::cash).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(3, objVendors.size());
        assertEquals(0, objCash.compareTo(objDay.totalCash()));
        assertEquals(0, new BigDecimal("108").compareTo(objDay.totalFuelGallons()));
        assertEquals(objDay.products().stream().mapToInt(prmProduct -> prmProduct.unitsSold()).sum(), objDay.totalProductUnits());
    }

    @Test
    void oneDateHasNoData() {
        assertTrue(atrStub.findDaySummary(SellerCloseQueryStub.NO_DATA_DATE).isEmpty());
    }
}
