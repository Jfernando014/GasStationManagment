package com.edu.unicauca.gasstation.backend.sales.exception;

public class JournalWorkbookGenerationException extends RuntimeException {

    public JournalWorkbookGenerationException(Throwable cause) {
        super("No fue posible generar el archivo Excel de la jornada", cause);
    }
}