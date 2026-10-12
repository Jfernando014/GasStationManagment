package com.edu.unicauca.gasstation.backend.sales.domain.models;

/**
 * Fuel product vocabulary from the Excel. This does not enable product-specific targets.
 * MAXPRO follows the Excel name; inventory currently uses MAX_PRO. No mapping is defined here.
 */
public enum FuelProduct {
    MOTOR,
    DIESEL,
    MAXPRO,
    EXTRA
}
