package com.edu.unicauca.gasstation.backend.workers.exception;

import com.edu.unicauca.gasstation.backend.workers.api.WorkerController;
import com.edu.unicauca.gasstation.backend.workers.api.dtos.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates the errors of {@link WorkerController} to HTTP responses with an {@link ErrorResponse} body.
 * Limited to this controller; other modules keep their own handling.
 */
@RestControllerAdvice(assignableTypes = WorkerController.class)
public class WorkersExceptionHandler {

    /** Body validation failed (400). Reports the first invalid field. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        FieldError error = ex.getBindingResult().getFieldError();
        ErrorResponse body = error == null
                ? ErrorResponse.of("La solicitud no es válida")
                : new ErrorResponse(error.getDefaultMessage(), error.getField());
        return ResponseEntity.badRequest().body(body);
    }

    /** The role sent does not exist (400). */
    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRoleNotFound(RoleNotFoundException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(ex.getMessage()));
    }

    /** The worker does not exist (404). */
    @ExceptionHandler(WorkerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWorkerNotFound(WorkerNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(ex.getMessage()));
    }

    /**
     * The document belongs to another worker (409). Raised by the service check, or by the service when the
     * database UNIQUE constraint rejects a concurrent duplicate.
     */
    @ExceptionHandler(DuplicateDocumentException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateDocument(DuplicateDocumentException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(ex.getMessage()));
    }
}
