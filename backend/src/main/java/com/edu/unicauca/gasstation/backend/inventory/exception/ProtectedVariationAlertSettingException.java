package com.edu.unicauca.gasstation.backend.inventory.exception;

public class ProtectedVariationAlertSettingException extends RuntimeException {

    public ProtectedVariationAlertSettingException() {
        super("Debe existir una configuración que cubra todos los meses; solo puedes editar sus valores");
    }
}
