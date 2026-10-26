package com.edu.unicauca.gasstation.backend.incentives.exception;

/** Errors of the module: stable code ({@link #name()}) and Spanish message template. */
public enum IncentiveError {

    PRODUCT_NOT_FOUND("No se encontró el producto con id %s"),
    PRODUCT_NOT_ADDITIVE("El producto '%s' no es un aditivo; solo los aditivos tienen regla de incentivo"),
    PRODUCT_INACTIVE("El aditivo '%s' está inactivo; no se pueden crear reglas nuevas para él"),
    INVALID_VALUE_PER_UNIT("El valor por unidad debe ser un número entero de pesos entre 0 y %s"),
    VALID_FROM_NOT_AFTER_LAST_CLOSED("La fecha de vigencia debe ser posterior a la última jornada cerrada (%s)"),
    RULE_ALREADY_EXISTS("El aditivo '%s' ya tiene una regla con vigencia desde el %s; las reglas no se editan, "
            + "elija otra fecha de vigencia");

    private final String messageTemplate;

    IncentiveError(String messageTemplate) {
        this.messageTemplate = messageTemplate;
    }

    public String message(Object... args) {
        return messageTemplate.formatted(args);
    }
}
