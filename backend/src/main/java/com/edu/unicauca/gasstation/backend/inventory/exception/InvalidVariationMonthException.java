package com.edu.unicauca.gasstation.backend.inventory.exception;

public class InvalidVariationMonthException extends RuntimeException {

    public InvalidVariationMonthException() {
        super("El mes debe tener el formato aaaa-mm, por ejemplo 2026-08");
    }
}
