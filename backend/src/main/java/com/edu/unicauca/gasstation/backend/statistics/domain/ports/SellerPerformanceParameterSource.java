package com.edu.unicauca.gasstation.backend.statistics.domain.ports;

import com.edu.unicauca.gasstation.backend.statistics.domain.calculation.SellerPerformance.*;
import java.util.List;

/**
 * Internal integration port for verified, versioned performance parameters. Implementations must use
 * public producer contracts, preserve provenance and operational dates, and never infer past periods
 * from today's catalog. Empty lists mean unavailable parameters, not zero targets or identity factors.
 * No production adapter is registered until those producer contracts and rules exist.
 */
@FunctionalInterface
public interface SellerPerformanceParameterSource {
    Snapshot read(Window window, Measure measure);

    record Snapshot(List<IndividualTarget> targets, List<ShiftClassification> classifications,
                    List<ShiftMultiplier> multipliers) {
        public Snapshot {
            targets = List.copyOf(targets);
            classifications = List.copyOf(classifications);
            multipliers = List.copyOf(multipliers);
        }
        public static Snapshot unavailable() { return new Snapshot(List.of(), List.of(), List.of()); }
    }
}
