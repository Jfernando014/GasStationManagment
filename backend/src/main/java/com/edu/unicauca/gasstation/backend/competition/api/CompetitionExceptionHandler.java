package com.edu.unicauca.gasstation.backend.competition.api;

import com.edu.unicauca.gasstation.backend.competition.exception.*;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice(assignableTypes = CompetitionController.class)
public class CompetitionExceptionHandler extends ResponseEntityExceptionHandler {
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var body = problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid request fields");
        body.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of("field", e.getField(), "message", String.valueOf(e.getDefaultMessage()))).toList());
        return handleExceptionInternal(ex, body, headers, status, request);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail invalid(IllegalArgumentException ex) { return problem(HttpStatus.BAD_REQUEST, "INVALID_COMPETITION_REQUEST", ex.getMessage()); }
    @ExceptionHandler({CompetitorNotFoundException.class, ObservationNotFoundException.class})
    public ProblemDetail missing(RuntimeException ex) { return problem(HttpStatus.NOT_FOUND, "COMPETITION_NOT_FOUND", ex.getMessage()); }
    @ExceptionHandler(InactiveCompetitorException.class)
    public ProblemDetail inactive(InactiveCompetitorException ex) { return problem(HttpStatus.CONFLICT, "INACTIVE_COMPETITOR", ex.getMessage()); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integrity(DataIntegrityViolationException ex) { return problem(HttpStatus.CONFLICT, "COMPETITION_DATA_CONFLICT", "Data conflicts with the competitor survey constraints"); }
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail forbidden(AccessDeniedException ex) { return problem(HttpStatus.FORBIDDEN, "ACCESS_DENIED", ex.getMessage()); }
    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ProblemDetail unauthenticated(AuthenticationCredentialsNotFoundException ex) { return problem(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", ex.getMessage()); }
    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        var result = ProblemDetail.forStatusAndDetail(status, detail);
        result.setTitle(status.getReasonPhrase()); result.setProperty("code", code);
        return result;
    }
}
