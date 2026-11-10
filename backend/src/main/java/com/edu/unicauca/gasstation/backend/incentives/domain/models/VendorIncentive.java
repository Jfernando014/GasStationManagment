package com.edu.unicauca.gasstation.backend.incentives.domain.models;

import java.math.BigDecimal;
import java.util.List;

/**
 * Incentive of one seller for a month. {@code accumulatedTotal} adds every month from the start of the
 * accumulation up to this one. {@code accumulatedUnresolvedUnits} counts the units of that whole period, this month
 * included, that could not be valued and are therefore missing from the accumulated total; zero means it is
 * complete. The name is null when the seller has no sales at all in that period.
 */
public record VendorIncentive(String vendorDocument, String vendorName, List<IncentiveLine> lines,
                              List<UnresolvedAdditive> unresolved, BigDecimal monthTotal,
                              BigDecimal accumulatedTotal, int accumulatedUnresolvedUnits) {
}
