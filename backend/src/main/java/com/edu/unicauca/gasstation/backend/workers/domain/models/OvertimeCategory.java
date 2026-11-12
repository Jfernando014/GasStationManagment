package com.edu.unicauca.gasstation.backend.workers.domain.models;

/**
 * Categories of the fortnightly overtime sheet. RDF, RNO and RNF are the surcharges worked out from the hours kept
 * per day; the four overtime ones (HEDO, HENO, HEDF, HENF) are shown but not calculated: they only change through
 * the fixed overtime line and the manual adjustments.
 */
public enum OvertimeCategory {
    /** Daytime hours of a Sunday or holiday. */
    RDF,
    /** Night hours of an ordinary day. */
    RNO,
    /** Night hours of a Sunday or holiday. */
    RNF,
    HEDO,
    HENO,
    HEDF,
    HENF
}
