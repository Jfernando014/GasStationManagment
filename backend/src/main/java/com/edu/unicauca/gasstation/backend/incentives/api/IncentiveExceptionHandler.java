package com.edu.unicauca.gasstation.backend.incentives.api;

import com.edu.unicauca.gasstation.backend.incentives.exception.IncentiveError;
import com.edu.unicauca.gasstation.backend.incentives.exception.IncentiveException;
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

/** Turns the errors of this API into problem details with a Spanish {@code detail} and a stable {@code code}. */
@RestControllerAdvice(basePackageClasses = IncentiveRuleController.class)
public class IncentiveExceptionHandler extends ResponseEntityExceptionHandler {

    /** Body failed validation (400); lists the invalid fields. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "La solicitud contiene datos no válidos");
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(IncentiveExceptionHandler::toFieldError)
                .toList();
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** Body is not valid JSON (400). */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "El cuerpo de la solicitud no es válido");
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** A path or query parameter has the wrong type (400). */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "El valor del parámetro '" + ex.getPropertyName() + "' no es válido");
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** Errors of the module. */
    @ExceptionHandler(IncentiveException.class)
    public ProblemDetail handleIncentiveException(IncentiveException ex) {
        return problem(statusOf(ex.error()), ex.error().name(), ex.getMessage());
    }

    private static HttpStatus statusOf(IncentiveError error) {
        return switch (error) {
            case PRODUCT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case PRODUCT_NOT_ADDITIVE, INVALID_VALUE_PER_UNIT -> HttpStatus.BAD_REQUEST;
            case PRODUCT_INACTIVE, VALID_FROM_NOT_AFTER_LAST_CLOSED, RULE_ALREADY_EXISTS -> HttpStatus.CONFLICT;
        };
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
