package com.edu.unicauca.gasstation.backend.sales.domain.profit;

/** Where the card percentage of a month comes from. */
public enum CardPercentageMode {
    /** Calculated from the card and total sales of the month. */
    REAL,
    /** Typed by the administrator. */
    MANUAL
}
