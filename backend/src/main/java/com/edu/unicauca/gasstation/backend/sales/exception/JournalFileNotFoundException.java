package com.edu.unicauca.gasstation.backend.sales.exception;

public class JournalFileNotFoundException extends RuntimeException {

    public JournalFileNotFoundException(Long fileId) {
        super("No existe el archivo de jornada con id " + fileId);
    }
}