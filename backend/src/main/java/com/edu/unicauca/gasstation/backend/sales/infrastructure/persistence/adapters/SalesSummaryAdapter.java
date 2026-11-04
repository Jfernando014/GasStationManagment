package com.edu.unicauca.gasstation.backend.sales.infrastructure.persistence.adapters;

import com.edu.unicauca.gasstation.backend.inventory.FuelType;
import com.edu.unicauca.gasstation.backend.inventory.SalesSummary;
import com.edu.unicauca.gasstation.backend.inventory.SalesSummaryPort;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.DaySummary;
import com.edu.unicauca.gasstation.backend.sales.SellerCloseQuery.ProductSaleSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Publishes the sales certified in step 2 in the terms of Inventory, so the journal step 5
 * calculates the variation and the product stock without typing the sales again.
 */
@Component
@RequiredArgsConstructor
public class SalesSummaryAdapter implements SalesSummaryPort {

    private final SellerCloseQuery sellerCloseQuery;

    @Override
    public Optional<SalesSummary> findByJournalDate(LocalDate journalDate) {
        if (journalDate == null) {
            return Optional.empty();
        }
        return sellerCloseQuery.findDaySummary(journalDate).map(this::toSummary);
    }

    private SalesSummary toSummary(DaySummary day) {
        return new SalesSummary(day.date(), gallonsByFuel(day), unitsByProduct(day));
    }

    /**
     * Gallons per fuel. A fuel with no sales is kept as zero because the day does have data:
     * the variation must show it as sold zero instead of as missing sales.
     */
    private Map<FuelType, BigDecimal> gallonsByFuel(DaySummary day) {
        Map<FuelType, BigDecimal> gallons = new EnumMap<>(FuelType.class);
        put(gallons, FuelType.MOTOR, day.gallonsMotor());
        put(gallons, FuelType.DIESEL, day.gallonsDiesel());
        put(gallons, FuelType.EXTRA, day.gallonsExtra());
        put(gallons, FuelType.MAX_PRO, day.gallonsMaxpro());
        return gallons;
    }

    private void put(Map<FuelType, BigDecimal> gallons, FuelType fuelType, BigDecimal value) {
        if (value != null) {
            gallons.put(fuelType, value);
        }
    }

    /**
     * Units sold keyed by SKU. Inventory matches them against the code of its catalog.
     */
    private Map<String, Integer> unitsByProduct(DaySummary day) {
        Map<String, Integer> units = new HashMap<>();
        for (ProductSaleSummary product : day.products()) {
            if (product.sku() == null || product.sku().isBlank() || product.unitsSold() <= 0) {
                continue;
            }
            units.merge(product.sku(), product.unitsSold(), Integer::sum);
        }
        return units;
    }
}
