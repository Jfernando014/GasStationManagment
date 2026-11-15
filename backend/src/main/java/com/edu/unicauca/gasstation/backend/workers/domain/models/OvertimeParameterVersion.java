package com.edu.unicauca.gasstation.backend.workers.domain.models;

import java.time.LocalDate;

/**
 * A version of the overtime parameters in the history. {@code validTo} is the last day it applies, the day before the
 * next version starts, and null when no later version exists. {@code current} marks the one in force today.
 */
public record OvertimeParameterVersion(OvertimeParameters parameters, LocalDate validTo, boolean current) {
}
