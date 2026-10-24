package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.exception.InvalidToleranceConfigurationException;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = DiscrepancyToleranceController.class)
public class DiscrepancyToleranceExceptionHandler {

    @ExceptionHandler(InvalidToleranceConfigurationException.class)
    public ProblemDetail handleInvalidConfiguration(InvalidToleranceConfigurationException ex) {
        return buildProblem(HttpStatus.BAD_REQUEST, "INVALID_TOLERANCE_CONFIG", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = buildProblem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request contains invalid fields");
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        problem.setProperty("errors", errors);
        return problem;
    }

    private ProblemDetail buildProblem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setType(URI.create("urn:problem-type:sales-tolerance:" + code.toLowerCase()));
        problem.setProperty("code", code);
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    private Map<String, String> toFieldError(FieldError fe) {
        Map<String, String> err = new LinkedHashMap<>();
        err.put("field", fe.getField());
        err.put("message", fe.getDefaultMessage());
        return err;
    }
}
