package com.edu.unicauca.gasstation.backend.shifts.exception;

/**
 * The shift code belongs to a role different from the worker's role. Translated to 409.
 */
public class ShiftRoleMismatchException extends RuntimeException {

    public ShiftRoleMismatchException(String shiftCode, String shiftRoleName) {
        super("El turno " + shiftCode + " es para el rol " + shiftRoleName + " y no corresponde al rol del vendedor");
    }
}
