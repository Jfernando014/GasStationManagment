package com.edu.unicauca.gasstation.backend.incentives.domain.models;

/** Why units sold could not be turned into pesos. Those units are reported but not added to any total. */
public enum UnresolvedReason {

    PRODUCT_NOT_FOUND("El SKU no corresponde a ningún producto del catálogo"),
    NO_RULE_IN_FORCE("El aditivo no tenía una regla de incentivo vigente en la fecha de la venta");

    private final String message;

    UnresolvedReason(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
