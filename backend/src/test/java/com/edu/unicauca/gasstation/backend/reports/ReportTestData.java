package com.edu.unicauca.gasstation.backend.reports;

import com.edu.unicauca.gasstation.backend.reports.domain.models.BalanceReport;
import com.edu.unicauca.gasstation.backend.reports.domain.models.DominusComplementReport;
import com.edu.unicauca.gasstation.backend.reports.domain.models.ReportDay;
import com.edu.unicauca.gasstation.backend.reports.domain.models.VariationReport;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo.ExpensesInfo;
import com.edu.unicauca.gasstation.backend.sales.CashBalanceTableInfo.SavingsFundInfo;
import com.edu.unicauca.gasstation.backend.sales.MigratedHistoryQuery;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Hand-written data shared by the reports tests.
 *
 * @author javiersolanop777
 */
public final class ReportTestData {

    public static final LocalDate DATE = LocalDate.of(2026, 8, 1);
    public static final LocalDate NEXT_DATE = LocalDate.of(2026, 8, 2);

    private ReportTestData() {
    }

    public static ReportDay liveDay() {
        return new ReportDay(DATE, ReportDay.Source.LIVE, bd("1000"), bd("1200000"), bd("900000"),
                bd("57000"), bd("-531"), List.of(new ReportDay.FuelSale("MOTOR", bd("480"))),
                List.of(vendor()), List.of(variation("MOTOR", "12.50")), cash(), 2);
    }

    public static ReportDay migratedDay() {
        return new ReportDay(NEXT_DATE, ReportDay.Source.MIGRATED, bd("800"), bd("1000000"), null,
                bd("50000"), bd("0"), List.of(new ReportDay.FuelSale("DIESEL", bd("315"))),
                List.of(vendor()), List.of(variation("DIESEL", "-5.80")), migratedCash(), 0);
    }

    public static ReportDay.FuelVariation variation(String fuel, String variance) {
        return new ReportDay.FuelVariation(fuel, bd("4500"), BigDecimal.ZERO, bd("480"),
                bd("4020"), bd("4032.50"), bd(variance), bd("65"), false, "OK");
    }

    public static ReportDay.VendorRow vendor() {
        return new ReportDay.VendorRow("1001", "Carlos M.", "Día", bd("310"), bd("57000"),
                bd("569469"), bd("570000"), bd("-531"), "FALTA");
    }

    public static ReportDay.CashSummary cash() {
        return new ReportDay.CashSummary(289L, "CLOSED", bd("7000000"), bd("900000"),
                bd("7900000"), bd("7899469"), bd("-531"), bd("6800000"), bd("3000000"));
    }

    public static ReportDay.CashSummary migratedCash() {
        return new ReportDay.CashSummary(290L, "MIGRATED", null, null, null,
                bd("14220000"), null, bd("6910000"), bd("3250000"));
    }

    public static VariationReport variationReport() {
        return new VariationReport(DATE, ReportDay.Source.LIVE, List.of(variation("MOTOR", "12.50")), 2);
    }

    public static DominusComplementReport dominusReport() {
        return new DominusComplementReport(DATE, ReportDay.Source.LIVE, cash(), List.of(vendor()),
                bd("569469"), bd("570000"), bd("-531"), bd("900000"), bd("57000"));
    }

    public static BalanceReport balanceReport() {
        return new BalanceReport(DATE, NEXT_DATE, List.of(liveDay(), migratedDay()), 1, 1,
                bd("1800"), bd("2200000"), bd("900000"), bd("107000"), bd("-531"),
                List.of(new BalanceReport.FuelVariationAggregate("MOTOR", bd("4020"), bd("4032.50"),
                        bd("12.50"), bd("65"), false, 1)),
                List.of(new BalanceReport.SellerAggregate("1001", "Carlos M.", bd("310"),
                        bd("569469"), bd("57000"), bd("-531"))),
                cash());
    }

    public static SellerCloseQuery.DaySummary daySummary() {
        return new SellerCloseQuery.DaySummary(DATE, bd("900000"), bd("100000"), bd("200000"),
                bd("300000"), bd("0"), bd("1200000"), bd("570000"), bd("-531"),
                bd("480"), bd("315"), bd("120"), bd("85"), bd("1000"), bd("57000"), 5, List.of());
    }

    public static SellerCloseQuery.VendorCloseSummary vendorClose() {
        return new SellerCloseQuery.VendorCloseSummary(DATE, "1001", "Carlos M.", "Día", 1,
                bd("900000"), bd("100000"), bd("200000"), bd("300000"), BigDecimal.ZERO,
                bd("569469"), bd("480"), bd("315"), bd("120"), bd("85"), bd("57000"),
                bd("570000"), bd("-531"), "FALTA", List.of());
    }

    public static CashBalanceTableInfo cashTable() {
        return new CashBalanceTableInfo(DATE, 289L, "CLOSED",
                new SavingsFundInfo(bd("6800000"), bd("250000"), BigDecimal.ZERO, bd("7050000")),
                new ExpensesInfo(List.of(), BigDecimal.ZERO), bd("7000000"), bd("900000"),
                bd("7900000"), bd("7899469"), bd("-531"));
    }

    public static MigratedHistoryQuery.MigratedDay migratedQueryDay() {
        return new MigratedHistoryQuery.MigratedDay(NEXT_DATE,
                List.of(new MigratedHistoryQuery.VendorSale("1002", "Mónica D.", "Día", bd("245"),
                        bd("28500"), bd("4120000"), bd("4120000"), BigDecimal.ZERO, "CUADRA")),
                null,
                List.of(new MigratedHistoryQuery.InventoryVariation("DIESEL", bd("3800"), BigDecimal.ZERO,
                        bd("315"), bd("3485"), bd("3479.20"), bd("-5.80"), bd("55"), false, "OK")),
                new MigratedHistoryQuery.CashSummary(290L, bd("6910000"), bd("3250000"), bd("14220000")));
    }

    public static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
