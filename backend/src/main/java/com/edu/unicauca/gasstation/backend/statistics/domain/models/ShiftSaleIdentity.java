package com.edu.unicauca.gasstation.backend.statistics.domain.models;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Business key of a seller close: operational date, original vendor identifier and source shift name.
 * Matches the source uniqueness without depending on a close UUID that the public query does not expose.
 * Text is preserved exactly as reported; vendorDocument is not assumed to be a worker UUID or a cedula.
 * Persistence must enforce this key to prevent concurrent duplicate inserts in a later increment.
 */
public record ShiftSaleIdentity(LocalDate journalDate, String vendorDocument, String shiftName) {

    public ShiftSaleIdentity {
        Objects.requireNonNull(journalDate, "journalDate");
        Objects.requireNonNull(vendorDocument, "vendorDocument");
        Objects.requireNonNull(shiftName, "shiftName");
        if (vendorDocument.isBlank()) {
            throw new IllegalArgumentException("vendorDocument must not be blank");
        }
        if (shiftName.isBlank()) {
            throw new IllegalArgumentException("shiftName must not be blank");
        }
    }
}
