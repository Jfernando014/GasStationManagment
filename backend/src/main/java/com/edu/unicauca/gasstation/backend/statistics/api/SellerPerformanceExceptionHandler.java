package com.edu.unicauca.gasstation.backend.statistics.api;

import com.edu.unicauca.gasstation.backend.statistics.exception.SellerPerformanceSourceException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice(assignableTypes = SellerPerformanceController.class)
public class SellerPerformanceExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail invalid(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_PERFORMANCE_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(SellerPerformanceSourceException.class)
    public ProblemDetail sourceFailure(SellerPerformanceSourceException ex) {
        // Do not expose SQL, documents or upstream exception messages in the HTTP response.
        return problem(HttpStatus.BAD_GATEWAY, "PERFORMANCE_SOURCE_FAILURE", ex.getMessage());
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
