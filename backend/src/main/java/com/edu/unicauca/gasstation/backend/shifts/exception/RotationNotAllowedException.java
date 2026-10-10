package com.edu.unicauca.gasstation.backend.shifts.exception;

/**
 * A rotation was requested for a worker who is not TITULAR. Translated to 409.
 * Support workers do not follow the 2x2 cycle; their days are assigned one by one.
 */
public class RotationNotAllowedException extends RuntimeException {

    public RotationNotAllowedException() {
        super("La rotación solo se genera para vendedores titulares; al apoyo se le asignan los días uno a uno");
    }
}
