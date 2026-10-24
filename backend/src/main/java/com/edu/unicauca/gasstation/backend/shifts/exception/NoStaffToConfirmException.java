package com.edu.unicauca.gasstation.backend.shifts.exception;

import java.time.LocalDate;

/**
 * Step 1 cannot be confirmed because nobody would work that day (VAL-D7). Translated to 409.
 */
public class NoStaffToConfirmException extends RuntimeException {

    public NoStaffToConfirmException(LocalDate date) {
        super("No se puede confirmar el personal del " + date + ": no queda ningún vendedor trabajando");
    }
}
