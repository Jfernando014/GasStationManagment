package com.edu.unicauca.gasstation.backend.sales.exception;

public class JournalFileReadException extends RuntimeException {

    public JournalFileReadException(Throwable cause) {
        super("No fue posible leer el archivo enviado", cause);
    }
}