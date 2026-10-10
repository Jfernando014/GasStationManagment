package com.edu.unicauca.gasstation.backend.shifts.exception;

import com.edu.unicauca.gasstation.backend.shifts.api.ShiftCodeController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates the errors of {@link ShiftCodeController} to HTTP responses.
 */
@RestControllerAdvice(assignableTypes = ShiftCodeController.class)
public class ShiftExceptionHandler {

    /** The shift code does not exist (404). */
    @ExceptionHandler(ShiftCodeNotFoundException.class)
    public ProblemDetail handleNotFound(ShiftCodeNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
