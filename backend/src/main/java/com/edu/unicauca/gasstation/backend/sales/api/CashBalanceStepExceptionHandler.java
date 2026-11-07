package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.exception.CashBalanceNotReadyException;
import com.edu.unicauca.gasstation.backend.sales.exception.JournalNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Errors that step 4 raises when it is completed through {@code POST /api/v1/journals/{id}/steps/4/complete}.
 * It handles only these two exceptions, so it does not change how {@link JournalController} answers anything else.
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

    /** The journal id does not exist (404). */
    @ExceptionHandler(JournalNotFoundException.class)
    public ProblemDetail handleJournalNotFound(JournalNotFoundException ex) {
        return CashBalanceExceptionHandler.problem(HttpStatus.NOT_FOUND, "JOURNAL_NOT_FOUND", ex.getMessage());
    }
}
