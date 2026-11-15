package com.edu.unicauca.gasstation.backend.sales.domain.models;

/** How the sales sheet balance ("planilla") compares with the Dominus balance. */
public enum CashDifferenceStatus {
    /** Both balances are equal. */
    CUADRA,
    /** The planilla has more cash than Dominus: the difference (Dominus - planilla) is negative. */
    PLANILLA_MAYOR,
    /** The planilla has less cash than Dominus: the difference (Dominus - planilla) is positive. */
    PLANILLA_MENOR
}
