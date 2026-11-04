package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.exception.DailyDownloadException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Turns the errors of the daily downloads into HTTP answers.
 * The content type is set on purpose: the download endpoints only produce .xlsx, so without it
 * a client that sends "Accept: xlsx" (like Swagger) would get a 406 instead of the real error.
 *
 * @author javiersolanop777
 */
@RestControllerAdvice(assignableTypes = DailyDownloadController.class)
public class DailyDownloadExceptionHandler {

    @ExceptionHandler(DailyDownloadException.class)
    public ResponseEntity<ProblemDetail> handleDownload(DailyDownloadException prmException) {
        HttpStatus objStatus = switch (prmException.getReason()) {
            case NO_DATA -> HttpStatus.NOT_FOUND;
            case GENERATION_FAILED -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return problem(objStatus, "DAILY_DOWNLOAD_" + prmException.getReason().name(), prmException.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleBadDate(MethodArgumentTypeMismatchException prmException) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_DATE", "The date must use the format yyyy-MM-dd");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleForbidden(AccessDeniedException prmException) {
        return problem(HttpStatus.FORBIDDEN, "ACCESS_DENIED", prmException.getMessage());
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUnauthenticated(AuthenticationCredentialsNotFoundException prmException) {
        return problem(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", prmException.getMessage());
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus prmStatus, String prmCode, String prmDetail) {
        ProblemDetail objProblem = ProblemDetail.forStatusAndDetail(prmStatus, prmDetail);
        objProblem.setTitle(prmStatus.getReasonPhrase());
        objProblem.setProperty("code", prmCode);
        return ResponseEntity.status(prmStatus).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(objProblem);
    }
}
