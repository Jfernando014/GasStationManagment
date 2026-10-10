package com.edu.unicauca.gasstation.backend.workers.api;

import com.edu.unicauca.gasstation.backend.workers.exception.DuplicateDocumentException;
import com.edu.unicauca.gasstation.backend.workers.exception.RoleNotFoundException;
import com.edu.unicauca.gasstation.backend.workers.exception.WorkerNotFoundException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Translates the errors of the workers API into RFC 9457 problem details ({@code application/problem+json}).
 *
 * <p>Every response carries {@code status}, {@code title}, a {@code detail} in Spanish for the end user and a
 * stable {@code code} in English. Validation errors also carry {@code errors}: every invalid field with its
 * message. Spring fills in {@code instance} with the request path.
 *
 * <p>Limited to the controllers of this package; other modules keep their own handling.
 */
@RestControllerAdvice(basePackageClasses = WorkerController.class)
public class WorkersExceptionHandler extends ResponseEntityExceptionHandler {

    /** Request body failed bean validation (400). Lists every invalid field. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "La solicitud contiene datos no válidos");
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(WorkersExceptionHandler::toFieldError)
                .toList();
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** Request body is not valid JSON or does not match the expected types (400). */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "El cuerpo de la solicitud no es válido");
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** A path or query parameter has the wrong type, for example an id that is not a UUID (400). */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "El valor del parámetro '" + ex.getPropertyName() + "' no es válido");
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** The role sent does not exist (400). */
    @ExceptionHandler(RoleNotFoundException.class)
    public ProblemDetail handleRoleNotFound(RoleNotFoundException ex) {
        return problem(HttpStatus.BAD_REQUEST, "ROLE_NOT_FOUND", ex.getMessage());
    }

    /** The worker does not exist (404). */
    @ExceptionHandler(WorkerNotFoundException.class)
    public ProblemDetail handleWorkerNotFound(WorkerNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "WORKER_NOT_FOUND", ex.getMessage());
    }

    /**
     * The document belongs to another worker (409). Raised by the service check, or by the service when the
     * database UNIQUE constraint rejects a concurrent duplicate.
     */
    @ExceptionHandler(DuplicateDocumentException.class)
    public ProblemDetail handleDuplicateDocument(DuplicateDocumentException ex) {
        return problem(HttpStatus.CONFLICT, "DUPLICATE_DOCUMENT", ex.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("code", code);
        return problem;
    }

    private static Map<String, String> toFieldError(FieldError error) {
        Map<String, String> fieldError = new LinkedHashMap<>();
        fieldError.put("field", error.getField());
        fieldError.put("message", error.getDefaultMessage());
        return fieldError;
    }
}
