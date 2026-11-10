package com.edu.unicauca.gasstation.backend.incentives.domain.models;

import java.util.List;

/** The processing state of a journal date and the additive units stored for it. */
public record AdditiveSalesDay(JournalSync sync, List<AdditiveSaleUnits> sales) {
}
