package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.exception.CashBalanceNotReadyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Errors that step 4 raises when it is completed through {@code POST /api/v1/journals/{id}/steps/4/complete}.
 * {@link JournalExceptionHandler} already handles {@code JournalNotFoundException} for {@link JournalController};
 * this advice handles only {@link CashBalanceNotReadyException}.
 */
@RestControllerAdvice(assignableTypes = JournalController.class)
public class CashBalanceStepExceptionHandler {

    /** Something is missing before step 4 can be completed (409). Carries the list in {@code missing}. */
    @ExceptionHandler(CashBalanceNotReadyException.class)
    public ProblemDetail handleNotReady(CashBalanceNotReadyException ex) {
        ProblemDetail problem = CashBalanceExceptionHandler.problem(HttpStatus.CONFLICT, "CASH_BALANCE_NOT_READY",
                ex.getMessage());
        problem.setProperty("missing", ex.getMissing());
        return problem;
    }
}
