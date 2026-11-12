package com.edu.unicauca.gasstation.backend.workers.exception;

/** A role named in the overtime parameters does not exist in the shifts catalog. Translated to 400. */
public class UnknownParameterRoleException extends RuntimeException {

    public UnknownParameterRoleException(String roleName) {
        super("El rol '" + roleName + "' no existe en el catálogo de turnos");
    }
}
