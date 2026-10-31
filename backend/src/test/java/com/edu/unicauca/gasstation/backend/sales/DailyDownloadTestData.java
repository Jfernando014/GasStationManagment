package com.edu.unicauca.gasstation.backend.sales;

import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.DaySummary;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.ProductSaleSummary;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.VendorCloseSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Fixed data for the daily download tests: one vendor, four products, one date with data.
 * Group totals: ACEITES 60000, MERCANCIA VARIA 5000, ADITIVOS Y OTROS 35000, total 100000.
 *
 * @author javiersolanop777
 */
public final class DailyDownloadTestData {

    public static final LocalDate DATE = LocalDate.of(2026, 10, 24);
    public static final LocalDate DATE_WITHOUT_DATA = LocalDate.of(2026, 1, 1);

    private DailyDownloadTestData() {
    }

    public static List<ProductSaleSummary> products() {
        return List.of(
                new ProductSaleSummary("80216", "Aceite Test", "LUBRICANTE", 3, bd("20000"), bd("60000")),
                new ProductSaleSummary("76102", "Aditivo Test", "ADITIVO", 2, bd("10000"), bd("20000")),
                new ProductSaleSummary("76001", "Bolsas Test", "OTRO", 5, bd("1000"), bd("5000")),
                new ProductSaleSummary("74066", "Liquido Frenos Test", "OTRO", 1, bd("15000"), bd("15000")));
    }

    public static DaySummary day() {
        return new DaySummary(DATE, bd("100000"), bd("20000"), bd("30000"), bd("10000"), bd("5000"),
                bd("100000"), bd("165000"), bd("0"), bd("10"), bd("20"), bd("5"), bd("0"), bd("35"),
                bd("100000"), 11, products());
    }

    public static VendorCloseSummary vendor() {
        return new VendorCloseSummary(DATE, "1061", "Vendedor Test", "DIA", 1, bd("100000"), bd("20000"),
                bd("30000"), bd("10000"), bd("5000"), bd("100000"), bd("10"), bd("20"), bd("5"), bd("0"),
                bd("100000"), bd("165000"), bd("0"), "OK", products());
    }

    public static BigDecimal bd(String prmValue) {
        return new BigDecimal(prmValue);
    }
}
