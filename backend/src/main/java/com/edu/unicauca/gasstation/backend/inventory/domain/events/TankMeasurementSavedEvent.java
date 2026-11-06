package com.edu.unicauca.gasstation.backend.inventory.domain.events;

import java.time.LocalDate;

/** Published inside the module once a tank measurement has been saved. */
public record TankMeasurementSavedEvent(LocalDate journalDate) {
}