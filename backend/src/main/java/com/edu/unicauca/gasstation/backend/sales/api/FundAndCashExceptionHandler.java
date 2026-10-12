package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.exception.DuplicateContributionException;
import com.edu.unicauca.gasstation.backend.sales.exception.InsufficientFundException;
import com.edu.unicauca.gasstation.backend.sales.exception.InvalidCashMovementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {SavingsFundController.class, CashController.class})
public class FundAndCashExceptionHandler {

    @ExceptionHandler({DuplicateContributionException.class, InsufficientFundException.class})
    public ResponseEntity<ProblemDetail> handleConflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(InvalidCashMovementException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCashMovement(InvalidCashMovementException ex) {
        return ResponseEntity.badRequest()
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }
}
