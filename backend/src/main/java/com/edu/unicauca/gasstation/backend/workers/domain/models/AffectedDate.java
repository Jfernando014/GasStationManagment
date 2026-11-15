package com.edu.unicauca.gasstation.backend.workers.domain.models;

import java.time.LocalDate;

/** A day of the fortnight whose hours are missing, pending or kept without their novelties. */
public record AffectedDate(LocalDate date, IncompleteReason reason) {
}
