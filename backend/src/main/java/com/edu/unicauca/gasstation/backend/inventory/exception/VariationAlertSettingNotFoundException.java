package com.edu.unicauca.gasstation.backend.inventory.exception;

import java.time.LocalDate;

public class VariationAlertSettingNotFoundException extends RuntimeException {

    public VariationAlertSettingNotFoundException(Long id) {
        super("No existe la configuración de alertas con id " + id);
    }

    public VariationAlertSettingNotFoundException(LocalDate date) {
        super("No hay una configuración de alertas vigente el " + date);
    }
}
