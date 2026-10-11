package com.edu.unicauca.gasstation.backend.sales;

import com.edu.unicauca.gasstation.backend.sales.domain.models.*;
import java.math.BigDecimal;
import java.time.Instant;

public final class SalesTargetTestData {
    private SalesTargetTestData() { }

    public static SalesTarget fuel(Integer month, String value) {
        return new SalesTarget(new SalesTargetIdentity(new SalesTargetPeriod(2026, month),
                SalesTargetCategory.FUEL_AGGREGATE), new BigDecimal(value), SalesTargetUnit.GALLONS);
    }

    public static StoredSalesTarget stored(SalesTarget target) {
        return new StoredSalesTarget(1L, target, Instant.parse("2026-08-01T00:00:00Z"));
    }
}
