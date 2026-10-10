package com.edu.unicauca.gasstation.backend.workers.api;

import com.edu.unicauca.gasstation.backend.workers.api.dtos.ErrorResponse;
import com.edu.unicauca.gasstation.backend.workers.domain.Worker;
import com.edu.unicauca.gasstation.backend.workers.exception.DuplicateDocumentException;
import com.edu.unicauca.gasstation.backend.workers.exception.RoleNotFoundException;
import com.edu.unicauca.gasstation.backend.workers.exception.WorkerNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
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

    /** The document belongs to another worker (409). */
    @ExceptionHandler(DuplicateDocumentException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateDocument(DuplicateDocumentException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(ex.getMessage()));
    }

    /**
     * Last line of defense for the unique document: two concurrent requests passed the service check
     * and the database rejected the second one (409, same message). Any other integrity error is rethrown.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        String cause = ex.getMostSpecificCause().getMessage();
        if (cause != null && cause.contains(Worker.UNIQUE_DOCUMENT_CONSTRAINT)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(DuplicateDocumentException.MESSAGE));
        }
        throw ex;
    }
}
