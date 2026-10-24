package com.edu.unicauca.gasstation.backend.shifts.exception;

/**
 * A worker would work a code of another role without marking it as exceptional coverage (VAL-D5). Translated to 409.
 */
public class ExceptionalCoverageRequiredException extends RuntimeException {

    public ExceptionalCoverageRequiredException(String fullName, String shiftCode, String shiftRoleName) {
        super("El turno " + shiftCode + " es de " + shiftRoleName + " y no corresponde al rol de " + fullName
                + "; márquelo como cobertura excepcional");
    }
}
