package com.edu.unicauca.gasstation.backend.sales.infrastructure.stub;

import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * TEST ONLY. Fake implementation of SellerCloseQuery for black-box tests without the real data.
 * It is active only with the Spring profile "stub". Every stub of the project must use this same
 * profile, so one setting (SPRING_PROFILES_ACTIVE=stub) turns on all of them.
 * It returns the same synthetic data (3 vendors, names start with [STUB]) for every date,
 * except 2000-01-01, which has no data, so the 404 answer can be tested.
 * Delete this class when the real implementation of SellerCloseQuery exists.
 *
 * @author javiersolanop777
 */
@Primary 
@Component
@Profile("stub")
public class SellerCloseQueryStub implements SellerCloseQuery {

    /** The only date without data. */
    public static final LocalDate NO_DATA_DATE = LocalDate.of(2000, 1, 1);

    @Override
    public Optional<DaySummary> findDaySummary(LocalDate prmDate) {
        if (NO_DATA_DATE.equals(prmDate)) {
            return Optional.empty();
        }
        List<VendorCloseSummary> objVendors = vendors(prmDate);
        BigDecimal objMotor = total(objVendors, VendorCloseSummary::gallonsMotor);
        BigDecimal objDiesel = total(objVendors, VendorCloseSummary::gallonsDiesel);
        BigDecimal objExtra = total(objVendors, VendorCloseSummary::gallonsExtra);
        BigDecimal objMaxpro = total(objVendors, VendorCloseSummary::gallonsMaxpro);
        List<ProductSaleSummary> objProducts = mergeProducts(objVendors);
        int varUnits = objProducts.stream().mapToInt(ProductSaleSummary::unitsSold).sum();
        return Optional.of(new DaySummary(prmDate,
                total(objVendors, VendorCloseSummary::cash),
                total(objVendors, VendorCloseSummary::ga),
                total(objVendors, VendorCloseSummary::credibanco),
                total(objVendors, VendorCloseSummary::qr),
                total(objVendors, VendorCloseSummary::redemptions),
                total(objVendors, VendorCloseSummary::totalDelivered),
                total(objVendors, VendorCloseSummary::dominusTirilla),
                total(objVendors, VendorCloseSummary::realDiscrepancy),
                objMotor, objDiesel, objExtra, objMaxpro,
                objMotor.add(objDiesel).add(objExtra).add(objMaxpro),
                total(objVendors, VendorCloseSummary::basketDominusAmount),
                varUnits, objProducts));
    }

    @Override
    public List<VendorCloseSummary> findVendorClosesByDate(LocalDate prmDate) {
        return NO_DATA_DATE.equals(prmDate) ? List.of() : vendors(prmDate);
    }

    @Override
    public Optional<VendorCloseSummary> findVendorClose(LocalDate prmDate, String prmVendorDocument) {
        return findVendorClosesByDate(prmDate).stream()
                .filter(prmVendor -> prmVendor.vendorDocument().equals(prmVendorDocument))
                .findFirst();
    }

    private static List<VendorCloseSummary> vendors(LocalDate prmDate) {
        List<VendorCloseSummary> objVendors = new ArrayList<>();
        for (int varIndex = 1; varIndex <= 3; varIndex++) {
            List<ProductSaleSummary> objProducts = List.of(
                    product("STUB-OIL", "[STUB] Aceite 20W50", "LUBRICANTE", 2 * varIndex, "28500"),
                    product("STUB-ADD", "[STUB] Aditivo gasolina", "ADITIVO", varIndex, "12000"),
                    product("76001", "[STUB] Bolsas para combustible", "OTRO", varIndex, "1000"));
            BigDecimal objCash = money(150000L * varIndex);
            BigDecimal objGa = money(20000L * varIndex);
            BigDecimal objCredibanco = money(30000L * varIndex);
            BigDecimal objQr = money(10000L * varIndex);
            BigDecimal objRedemptions = money(5000L * varIndex);
            boolean varDiscrepancy = varIndex == 3;
            objVendors.add(new VendorCloseSummary(prmDate,
                    "STUB-000" + varIndex, "[STUB] Vendor " + varIndex, varIndex == 2 ? "NIGHT" : "DAY", varIndex,
                    objCash, objGa, objCredibanco, objQr, objRedemptions, objCash,
                    money(10L * varIndex), money(5L * varIndex), money(2L * varIndex), money(varIndex),
                    total(objProducts, ProductSaleSummary::totalAmount),
                    objCash.add(objGa).add(objCredibanco).add(objQr).add(objRedemptions),
                    varDiscrepancy ? money(500) : BigDecimal.ZERO,
                    varDiscrepancy ? "DISCREPANCY" : "OK",
                    objProducts));
        }
        return objVendors;
    }

    private static List<ProductSaleSummary> mergeProducts(List<VendorCloseSummary> prmVendors) {
        Map<String, ProductSaleSummary> objBySku = new LinkedHashMap<>();
        for (VendorCloseSummary objVendor : prmVendors) {
            for (ProductSaleSummary objProduct : objVendor.products()) {
                objBySku.merge(objProduct.sku(), objProduct, (prmFirst, prmSecond) -> new ProductSaleSummary(
                        prmFirst.sku(), prmFirst.productName(), prmFirst.category(),
                        prmFirst.unitsSold() + prmSecond.unitsSold(), prmFirst.unitPrice(),
                        prmFirst.totalAmount().add(prmSecond.totalAmount())));
            }
        }
        return new ArrayList<>(objBySku.values());
    }

    private static ProductSaleSummary product(String prmSku, String prmName, String prmCategory,
                                              int prmUnits, String prmPrice) {
        BigDecimal objPrice = new BigDecimal(prmPrice);
        return new ProductSaleSummary(prmSku, prmName, prmCategory, prmUnits, objPrice,
                objPrice.multiply(BigDecimal.valueOf(prmUnits)));
    }

    private static BigDecimal money(long prmValue) {
        return BigDecimal.valueOf(prmValue);
    }

    private static <T> BigDecimal total(List<T> prmItems, Function<T, BigDecimal> prmGetter) {
        return prmItems.stream().map(prmGetter).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
