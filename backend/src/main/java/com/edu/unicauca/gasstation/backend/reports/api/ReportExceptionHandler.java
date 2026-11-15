package com.edu.unicauca.gasstation.backend.reports.api;

import com.edu.unicauca.gasstation.backend.reports.exception.InvalidReportPeriodException;
import com.edu.unicauca.gasstation.backend.reports.exception.ReportGenerationException;
import com.edu.unicauca.gasstation.backend.reports.exception.ReportNoDataException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Turns the errors of the report downloads into HTTP Problem Details.
 *
 * @author javiersolanop777
 */
@RestControllerAdvice(assignableTypes = ReportController.class)
public class ReportExceptionHandler {

    @ExceptionHandler(ReportNoDataException.class)
    public ResponseEntity<ProblemDetail> handleNoData(ReportNoDataException exception) {
        return problem(HttpStatus.NOT_FOUND, "REPORT_NO_DATA", exception.getMessage());
    }

    @ExceptionHandler(InvalidReportPeriodException.class)
    public ResponseEntity<ProblemDetail> handleInvalidPeriod(InvalidReportPeriodException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_PERIOD", exception.getMessage());
    }

    @ExceptionHandler(ReportGenerationException.class)
    public ResponseEntity<ProblemDetail> handleGeneration(ReportGenerationException exception) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "REPORT_GENERATION_FAILED", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleBadDate(MethodArgumentTypeMismatchException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_DATE", "Las fechas deben usar el formato yyyy-MM-dd");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParameter(MissingServletRequestParameterException exception) {
        return problem(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER",
                "El parámetro '" + exception.getParameterName() + "' es obligatorio");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleForbidden(AccessDeniedException exception) {
        return problem(HttpStatus.FORBIDDEN, "ACCESS_DENIED", exception.getMessage());
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUnauthenticated(AuthenticationCredentialsNotFoundException exception) {
        return problem(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", exception.getMessage());
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("code", code);
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem);
    }
}
