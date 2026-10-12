package com.edu.unicauca.gasstation.backend.sales.exception;

public class SalesTargetNotFoundException extends RuntimeException {
    public SalesTargetNotFoundException() {
        super("No existe una meta para la consulta solicitada");
    }
}
