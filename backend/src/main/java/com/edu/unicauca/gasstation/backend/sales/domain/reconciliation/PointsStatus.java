package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

/** State of the Puntos Colombia step. */
public enum PointsStatus {
    /** Every planilla has 0 redemptions and no total was typed: the step completes with 0, nothing to type. */
    COMPLETED_ZERO,
    /** There are redemptions but the day total of Puntos Colombia has not been loaded yet. */
    PENDING_TOTAL,
    /** The day total equals the redemptions declared in the planillas ("Cuadra"). */
    MATCH,
    /** The day total differs from the redemptions of the planillas. */
    DIFFERENCE
}
