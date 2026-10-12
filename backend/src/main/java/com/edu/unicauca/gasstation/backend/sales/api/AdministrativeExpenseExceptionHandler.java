package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.exception.AdministrativeExpenseNotFoundException;
import com.edu.unicauca.gasstation.backend.sales.exception.InvalidAdministrativeExpenseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AdministrativeExpenseController.class)
public class AdministrativeExpenseExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "La solicitud contiene datos no válidos");
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(AdministrativeExpenseExceptionHandler::toFieldError)
                .toList();
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMalformedRequest(HttpMessageNotReadableException ex) {
        return problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "El cuerpo de la solicitud no es válido");
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ProblemDetail handleInvalidParameter(TypeMismatchException ex) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "El valor del parámetro no es válido");
    }

    @ExceptionHandler(InvalidAdministrativeExpenseException.class)
    public ProblemDetail handleInvalidExpense(InvalidAdministrativeExpenseException ex) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_ADMINISTRATIVE_EXPENSE", ex.getMessage());
    }

    @ExceptionHandler(AdministrativeExpenseNotFoundException.class)
    public ProblemDetail handleExpenseNotFound(AdministrativeExpenseNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "ADMINISTRATIVE_EXPENSE_NOT_FOUND", ex.getMessage());
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