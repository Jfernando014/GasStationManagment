package com.edu.unicauca.gasstation.backend.competition.exception;

public class CompetitorNotFoundException extends RuntimeException {
    public CompetitorNotFoundException(long id) { super("Competitor not found: " + id); }
}
