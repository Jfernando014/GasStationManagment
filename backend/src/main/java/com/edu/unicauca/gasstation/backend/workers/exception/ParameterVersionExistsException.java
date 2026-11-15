package com.edu.unicauca.gasstation.backend.workers.exception;

import java.time.LocalDate;

/** Another version of the overtime parameters already starts that day. Translated to 409. */
public class ParameterVersionExistsException extends RuntimeException {

    public ParameterVersionExistsException(LocalDate validFrom) {
        super("Ya existe una versión de los parámetros con vigencia desde el " + validFrom
                + "; las versiones no se editan, elija otra fecha de vigencia");
    }
}
