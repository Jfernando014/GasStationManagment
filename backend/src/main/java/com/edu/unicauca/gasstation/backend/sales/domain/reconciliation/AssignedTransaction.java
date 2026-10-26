package com.edu.unicauca.gasstation.backend.sales.domain.reconciliation;

import java.util.UUID;

/** A Redeban row with the vendor it was attributed to (null unless ASSIGNED). */
public record AssignedTransaction(RedebanTransaction transaction, UUID workerId, AssignmentStatus status) {
}
