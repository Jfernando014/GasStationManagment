package com.edu.unicauca.gasstation.backend.competition.exception;

public class ObservationNotFoundException extends RuntimeException {
    public ObservationNotFoundException(long id) { super("Price observation not found: " + id); }
}
