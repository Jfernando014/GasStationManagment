package com.edu.unicauca.gasstation.backend.workers.exception;

/** A parameter of the overtime sheet is outside its allowed range. Translated to 400. */
public class ParameterOutOfRangeException extends RuntimeException {

    public ParameterOutOfRangeException(String name, int min, int max) {
        super("El parámetro '" + name + "' debe ser un número entero entre " + min + " y " + max);
    }
}
