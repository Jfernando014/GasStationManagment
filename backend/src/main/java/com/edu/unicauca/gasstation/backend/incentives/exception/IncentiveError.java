package com.edu.unicauca.gasstation.backend.incentives.exception;

/** Errors of the module: stable code ({@link #name()}) and Spanish message template. */
public enum IncentiveError {

    PRODUCT_NOT_FOUND("El producto con id %s no existe o no es un aditivo"),
    PRODUCT_INACTIVE("El aditivo '%s' está inactivo; no se pueden crear reglas nuevas para él"),
    INVALID_VALUE_PER_UNIT("El valor por unidad debe ser un número entero de pesos entre 0 y %s"),
    VALID_FROM_NOT_AFTER_LAST_CLOSED("La fecha de vigencia debe ser posterior a la última jornada cerrada (%s)"),
    RULE_ALREADY_EXISTS("El aditivo '%s' ya tiene una regla con vigencia desde el %s; las reglas no se editan, "
            + "elija otra fecha de vigencia"),
    ADDITIVE_SALES_NOT_REGISTERED("No hay registro de ventas de aditivos para la fecha %s"),
    MONTH_IN_FUTURE("No se puede calcular el mes %s porque aún no llega; el último mes disponible es %s"),
    MONTH_BEFORE_START("No se puede calcular el mes %s: el cálculo de incentivos empieza en %s"),
    ADDITIVE_SALES_PROCESSING_FAILED("No se pudieron procesar las ventas de aditivos del %s; la fecha quedó "
            + "pendiente. Intente reprocesarla de nuevo");

    private final String messageTemplate;

    IncentiveError(String messageTemplate) {
        this.messageTemplate = messageTemplate;
    }

    public String message(Object... args) {
        return messageTemplate.formatted(args);
    }
}
