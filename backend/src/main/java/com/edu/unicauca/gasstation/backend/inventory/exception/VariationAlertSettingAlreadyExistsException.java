package com.edu.unicauca.gasstation.backend.inventory.exception;

import java.time.LocalDate;

public class VariationAlertSettingAlreadyExistsException extends RuntimeException {

    public VariationAlertSettingAlreadyExistsException(LocalDate validFrom) {
        super("Ya existe una configuración de alertas con fecha de vigencia " + validFrom);
    }
}
