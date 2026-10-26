package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

import java.math.BigDecimal;
import java.time.LocalTime;

/** HU-42 #2: a Redeban row that could not be attributed to a vendor, for example at a shift crossing. */
public record UnassignedTransaction(int row, LocalTime time, BigDecimal amount, String reference,
                                    AssignmentStatus status) {
}
