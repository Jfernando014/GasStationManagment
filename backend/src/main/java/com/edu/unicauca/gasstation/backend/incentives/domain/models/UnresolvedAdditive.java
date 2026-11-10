package com.edu.unicauca.gasstation.backend.incentives.domain.models;

/** Units of an additive that earned nothing because of {@code reason}; they are left out of the totals. */
public record UnresolvedAdditive(String sku, String productName, int units, UnresolvedReason reason) {
}
