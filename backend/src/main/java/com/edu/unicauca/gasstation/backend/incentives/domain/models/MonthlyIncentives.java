package com.edu.unicauca.gasstation.backend.incentives.domain.models;

import java.time.YearMonth;
import java.util.List;

/** Incentives of the sellers who sold additives in {@code month}, with the month the accumulation starts at. */
public record MonthlyIncentives(YearMonth month, YearMonth accumulatedFrom, List<VendorIncentive> vendors) {
}
