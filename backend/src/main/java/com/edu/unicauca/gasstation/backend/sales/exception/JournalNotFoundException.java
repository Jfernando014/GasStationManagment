package com.edu.unicauca.gasstation.backend.sales.exception;

public class JournalNotFoundException extends RuntimeException {

    public JournalNotFoundException(Long journalId) {
        super("No existe la jornada con id " + journalId);
    }
}