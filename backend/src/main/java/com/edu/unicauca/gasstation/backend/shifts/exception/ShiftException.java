package com.edu.unicauca.gasstation.backend.shifts.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Error of the shifts module. The {@link ShiftError} gives the stable code, the HTTP status and the message; the
 * {@code ShiftExceptionHandler} turns it into a problem detail. Extra properties, when an error needs them, are
 * added to the body of that answer.
 */
public class ShiftException extends RuntimeException {

    private final ShiftError error;
    private final Map<String, Object> properties = new LinkedHashMap<>();

    /**
     * @param args values for the placeholders of the message of {@code error}, in order
     */
    public ShiftException(ShiftError error, Object... args) {
        super(error.message(args));
        this.error = error;
    }

    public ShiftError error() {
        return error;
    }

    /** Adds a property to the body of the answer; returns this exception so it can be thrown right away. */
    public ShiftException withProperty(String name, Object value) {
        properties.put(name, value);
        return this;
    }

    /** Extra properties for the body of the answer, in insertion order. */
    public Map<String, Object> properties() {
        return Collections.unmodifiableMap(properties);
    }
}
