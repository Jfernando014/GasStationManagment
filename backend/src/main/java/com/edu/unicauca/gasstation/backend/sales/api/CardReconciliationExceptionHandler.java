package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.domain.reconciliation.ReconciliationValidationException;
import com.edu.unicauca.gasstation.backend.sales.exception.SalesParameterNotFoundException;
import com.fasterxml.jackson.databind.JsonMappingException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * RFC 9457 errors for the reconciliation and parameter endpoints. Every invalid value names its field (HU-34 #5):
 * bean validation lists all of them in {@code errors}; a value that is not a number reports the offending field too.
 */
@RestControllerAdvice(assignableTypes = {CredibancoReconciliationController.class, QrPointsReconciliationController.class,
        SalesParameterController.class})
public class CardReconciliationExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var problem = problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "La solicitud contiene datos no válidos");
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of("field", e.getField(), "message", String.valueOf(e.getDefaultMessage()))).toList();
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** Non-numeric or malformed body (400); names the field when Jackson can tell which one it was. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String field = null;
        if (ex.getCause() instanceof JsonMappingException mapping && !mapping.getPath().isEmpty()) {
            field = mapping.getPath().get(mapping.getPath().size() - 1).getFieldName();
        }
        var problem = problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", field == null
                ? "El cuerpo de la solicitud no es válido"
                : "El valor del campo '" + field + "' no es válido: se esperaba un número");
        if (field != null) {
            problem.setProperty("errors", List.of(Map.of("field", field, "message", "Debe ser un número")));
        }
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        var problem = problem(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "El valor del parámetro '" + ex.getPropertyName() + "' no es válido");
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(ReconciliationValidationException.class)
    public ProblemDetail invalidValue(ReconciliationValidationException ex) {
        var problem = problem(HttpStatus.BAD_REQUEST, "INVALID_RECONCILIATION_VALUE", ex.getMessage());
        problem.setProperty("errors", List.of(Map.of("field", ex.field(), "message", ex.getMessage())));
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail invalid(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_RECONCILIATION", ex.getMessage());
    }

    @ExceptionHandler(SalesParameterNotFoundException.class)
    public ProblemDetail parameterMissing(SalesParameterNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "SALES_PARAMETER_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail forbidden(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "ACCESS_DENIED", ex.getMessage());
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ProblemDetail unauthenticated(AuthenticationCredentialsNotFoundException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", ex.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("code", code);
        return problem;
    }
}
