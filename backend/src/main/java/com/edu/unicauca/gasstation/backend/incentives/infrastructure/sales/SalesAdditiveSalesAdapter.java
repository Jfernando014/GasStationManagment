package com.edu.unicauca.gasstation.backend.incentives.infrastructure.sales;

import com.edu.unicauca.gasstation.backend.incentives.domain.models.AdditiveSaleUnits;
import com.edu.unicauca.gasstation.backend.incentives.domain.ports.AdditiveSalesPort;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.ProductSaleSummary;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.VendorCloseSummary;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Reads the additive units of each seller from the seller closes that sales publishes. */
@Slf4j
@Component
@RequiredArgsConstructor
public class SalesAdditiveSalesAdapter implements AdditiveSalesPort {

    private static final String ADDITIVE_CATEGORY = "ADITIVO";

    private final SellerCloseQuery sellerCloseQuery;

    @Override
    public List<AdditiveSaleUnits> findByDate(LocalDate journalDate) {
        List<VendorCloseSummary> closes = sellerCloseQuery.findVendorClosesByDate(journalDate);
        // A seller can have several closes in one day (one per shift), so the same SKU may repeat
        Map<String, AdditiveSaleUnits> bySellerAndSku = new LinkedHashMap<>();
        for (VendorCloseSummary close : closes) {
            addAdditives(journalDate, close, bySellerAndSku);
        }
        return List.copyOf(bySellerAndSku.values());
    }

    private void addAdditives(LocalDate journalDate, VendorCloseSummary close, Map<String, AdditiveSaleUnits> sales) {
        if (isBlank(close.vendorDocument())) {
            if (close.products().stream().anyMatch(product -> isAdditive(product) && product.unitsSold() > 0)) {
                log.warn("Ignoring the additive sales of a seller close without document on {}", journalDate);
            }
            return;
        }
        String document = close.vendorDocument().strip();
        for (ProductSaleSummary product : close.products()) {
            if (isBlank(product.category())) {
                log.warn("Ignoring product {} of seller {} on {}: it has no category", product.sku(), document,
                        journalDate);
                continue;
            }
            if (!isAdditive(product) || product.unitsSold() <= 0) {
                continue;
            }
            if (isBlank(product.sku())) {
                log.warn("Ignoring additive {} of seller {} on {}: it has no SKU", product.productName(), document,
                        journalDate);
                continue;
            }
            String sku = product.sku().strip();
            AdditiveSaleUnits line = new AdditiveSaleUnits(journalDate, document,
                    nameOf(close.vendorName(), document), sku, nameOf(product.productName(), sku),
                    product.unitsSold());
            sales.merge(document + "|" + sku, line,
                    (current, added) -> new AdditiveSaleUnits(current.journalDate(), current.vendorDocument(),
                            current.vendorName(), current.sku(), current.productName(),
                            current.units() + added.units()));
        }
    }

    // Same comparison as the daily download export: ignores case and surrounding spaces
    private static boolean isAdditive(ProductSaleSummary product) {
        return product.category() != null && ADDITIVE_CATEGORY.equalsIgnoreCase(product.category().strip());
    }

    private static String nameOf(String name, String fallback) {
        return isBlank(name) ? fallback : name.strip();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
