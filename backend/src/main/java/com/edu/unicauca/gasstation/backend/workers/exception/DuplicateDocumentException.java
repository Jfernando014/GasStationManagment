package com.edu.unicauca.gasstation.backend.workers.exception;

import lombok.Getter;

/**
 * The document (cédula) already belongs to another worker. Translated to 409.
 */
@Getter
public class DuplicateDocumentException extends RuntimeException {

    public static final String MESSAGE = "El documento ya se encuentra registrado para otro trabajador";

    /** Document that caused the conflict, for logging; it is not part of the user message. */
    private final String document;

    public DuplicateDocumentException(String document) {
        super(MESSAGE);
        this.document = document;
    }
}
