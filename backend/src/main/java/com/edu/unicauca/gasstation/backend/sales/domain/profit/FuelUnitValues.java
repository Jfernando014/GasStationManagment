package com.edu.unicauca.gasstation.backend.sales.domain.profit;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Per gallon figures of a fuel in force on a date. A null component means that nothing was in force. */
public record FuelUnitValues(
        BigDecimal salePrice,
        BigDecimal purchaseCost,
        BigDecimal freightPerGallon,
        BigDecimal financialCostPerGallon) {

    public FuelUnitValues {
        salePrice = Decimals.plain(salePrice);
        purchaseCost = Decimals.plain(purchaseCost);
        freightPerGallon = Decimals.plain(freightPerGallon);
        financialCostPerGallon = Decimals.plain(financialCostPerGallon);
    }

    /** Sale price less purchase cost, freight and financial cost; null when any of them is missing. */
    public BigDecimal marginPerGallon() {
        if (!missing().isEmpty()) {
            return null;
        }
        return Decimals.plain(salePrice.subtract(purchaseCost).subtract(freightPerGallon)
                .subtract(financialCostPerGallon));
    }

    public List<MissingDatum> missing() {
        List<MissingDatum> missing = new ArrayList<>();
        if (salePrice == null) {
            missing.add(MissingDatum.SALE_PRICE);
        }
        if (purchaseCost == null) {
            missing.add(MissingDatum.PURCHASE_COST);
        }
        if (freightPerGallon == null) {
            missing.add(MissingDatum.FREIGHT_PER_GALLON);
        }
        if (financialCostPerGallon == null) {
            missing.add(MissingDatum.FINANCIAL_COST_PER_GALLON);
        }
        return missing;
    }
}
