package com.edu.unicauca.gasstation.backend.incentives.domain.models;

import java.time.LocalDate;

/**
 * Units of one additive that one seller sold on a journal date. The seller is identified by the document and the
 * additive by the SKU, both as sales reports them.
 */
public record AdditiveSaleUnits(LocalDate journalDate, String vendorDocument, String vendorName, String sku,
                                String productName, int units) {
}
