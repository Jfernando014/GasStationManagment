package com.edu.unicauca.gasstation.backend.workers.domain.models;

import java.util.List;

/** The version in force on a date, the ones already scheduled after it, and the whole history. */
public record OvertimeParameterOverview(OvertimeParameters active, List<OvertimeParameters> scheduled,
                                        List<OvertimeParameters> history) {
}
