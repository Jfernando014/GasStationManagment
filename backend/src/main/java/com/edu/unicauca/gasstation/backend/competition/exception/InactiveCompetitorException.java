package com.edu.unicauca.gasstation.backend.competition.exception;

public class InactiveCompetitorException extends RuntimeException {
    public InactiveCompetitorException(long id) { super("Cannot register observations for inactive competitor: " + id); }
}
