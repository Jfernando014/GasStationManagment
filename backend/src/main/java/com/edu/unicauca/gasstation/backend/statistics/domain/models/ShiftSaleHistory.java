package com.edu.unicauca.gasstation.backend.statistics.domain.models;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable historical sale of a resolved seller and effective shift on a closed operational day.
 * Stores values and role snapshots, never mutable worker or assignment entities.
 * workerId is resolved separately from the original vendorDocument; this model performs no lookup.
 * The role comes from the worked shift, not the worker's usual role or the need for support.
 */
public record ShiftSaleHistory(
        ClosedJournalSnapshot journal,
        ShiftSaleIdentity identity,
        UUID workerId,
        String vendorName,
        String workedCode,
        EffectiveRole effectiveRole,
        ShiftSaleAmounts sales) {

    public ShiftSaleHistory {
        Objects.requireNonNull(journal, "journal");
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(workerId, "workerId");
        Objects.requireNonNull(vendorName, "vendorName");
        Objects.requireNonNull(workedCode, "workedCode");
        Objects.requireNonNull(effectiveRole, "effectiveRole");
        Objects.requireNonNull(sales, "sales");
        if (!journal.journalDate().equals(identity.journalDate())) {
            throw new IllegalArgumentException("identity date must match the closed journal date");
        }
        if (workerId.equals(new UUID(0, 0))) {
            throw new IllegalArgumentException("workerId must not be the zero UUID");
        }
        if (vendorName.isBlank()) {
            throw new IllegalArgumentException("vendorName must not be blank");
        }
        if (workedCode.isBlank()) {
            throw new IllegalArgumentException("workedCode must not be blank");
        }
    }

    /** Derived from the stored effective role so the historical support flag cannot contradict it. */
    public boolean support() {
        return effectiveRole == EffectiveRole.APOYO;
    }

    /** Effective roles currently provided by the shift catalog. */
    public enum EffectiveRole {
        TITULAR,
        APOYO
    }
}
