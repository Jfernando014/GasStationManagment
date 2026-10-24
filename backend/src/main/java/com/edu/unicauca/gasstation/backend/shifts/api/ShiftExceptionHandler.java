package com.edu.unicauca.gasstation.backend.shifts.api;

import com.edu.unicauca.gasstation.backend.shifts.exception.ExceptionalCoverageRequiredException;
import com.edu.unicauca.gasstation.backend.shifts.exception.InvalidDateRangeException;
import com.edu.unicauca.gasstation.backend.shifts.exception.InvalidNoveltyException;
import com.edu.unicauca.gasstation.backend.shifts.exception.InvalidNoveltyReasonException;
import com.edu.unicauca.gasstation.backend.shifts.exception.InvalidRotationLengthException;
import com.edu.unicauca.gasstation.backend.shifts.exception.JournalNotEditableException;
import com.edu.unicauca.gasstation.backend.shifts.exception.JournalNotFoundException;
import com.edu.unicauca.gasstation.backend.shifts.exception.NoStaffToConfirmException;
import com.edu.unicauca.gasstation.backend.shifts.exception.NoveltyConflictException;
import com.edu.unicauca.gasstation.backend.shifts.exception.NoveltyNotFoundException;
import com.edu.unicauca.gasstation.backend.shifts.exception.PastDateException;
import com.edu.unicauca.gasstation.backend.shifts.exception.RotationNotAllowedException;
import com.edu.unicauca.gasstation.backend.shifts.exception.SameWorkerNoveltyException;
import com.edu.unicauca.gasstation.backend.shifts.exception.ScheduleWorkerNotFoundException;
import com.edu.unicauca.gasstation.backend.shifts.exception.ShiftCodeNotFoundException;
import com.edu.unicauca.gasstation.backend.shifts.exception.ShiftHoursOverlapException;
import com.edu.unicauca.gasstation.backend.shifts.exception.ShiftRoleMismatchException;
import com.edu.unicauca.gasstation.backend.shifts.exception.WorkerAlreadyWorkingException;
import com.edu.unicauca.gasstation.backend.shifts.exception.WorkerNotAvailableException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Translates the errors of the shifts API into RFC 9457 problem details ({@code application/problem+json}).
 *
 * <p>Every response carries {@code status}, {@code title}, a {@code detail} in Spanish for the end user and a
 * stable {@code code} in English. Validation errors also carry {@code errors}: every invalid field with its
 * message. Spring fills in {@code instance} with the request path.
 *
 * <p>Limited to the controllers of this package; other modules keep their own handling.
 */
@RestControllerAdvice(basePackageClasses = ShiftCodeController.class)
public class ShiftExceptionHandler extends ResponseEntityExceptionHandler {

    /** Request body failed bean validation (400). Lists every invalid field. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "La solicitud contiene datos no válidos");
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(ShiftExceptionHandler::toFieldError)
                .toList();
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** Request body is not valid JSON or does not match the expected types (400). */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "El cuerpo de la solicitud no es válido");
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** A path or query parameter has the wrong type (400). */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "El valor del parámetro '" + ex.getPropertyName() + "' no es válido");
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** The shift code does not exist (404). */
    @ExceptionHandler(ShiftCodeNotFoundException.class)
    public ProblemDetail handleShiftCodeNotFound(ShiftCodeNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "SHIFT_CODE_NOT_FOUND", ex.getMessage());
    }

    /** A shift was scheduled on a past day (400). */
    @ExceptionHandler(PastDateException.class)
    public ProblemDetail handlePastDate(PastDateException ex) {
        return problem(HttpStatus.BAD_REQUEST, "PAST_DATE", ex.getMessage());
    }

    /** The end of a date range is before its start (400). */
    @ExceptionHandler(InvalidDateRangeException.class)
    public ProblemDetail handleInvalidDateRange(InvalidDateRangeException ex) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", ex.getMessage());
    }

    /** The rotation range is shorter than one full cycle or longer than a quarter (400). */
    @ExceptionHandler(InvalidRotationLengthException.class)
    public ProblemDetail handleInvalidRotationLength(InvalidRotationLengthException ex) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_ROTATION_LENGTH", ex.getMessage());
    }

    /** The worker to schedule does not exist (404). */
    @ExceptionHandler(ScheduleWorkerNotFoundException.class)
    public ProblemDetail handleWorkerNotFound(ScheduleWorkerNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "WORKER_NOT_FOUND", ex.getMessage());
    }

    /** The worker to schedule is inactive (409). */
    @ExceptionHandler(WorkerNotAvailableException.class)
    public ProblemDetail handleWorkerNotAvailable(WorkerNotAvailableException ex) {
        return problem(HttpStatus.CONFLICT, "WORKER_INACTIVE", ex.getMessage());
    }

    /** The shift code belongs to a role different from the worker's role (409). */
    @ExceptionHandler(ShiftRoleMismatchException.class)
    public ProblemDetail handleRoleMismatch(ShiftRoleMismatchException ex) {
        return problem(HttpStatus.CONFLICT, "ROLE_MISMATCH", ex.getMessage());
    }

    /** A rotation was requested for a worker who is not TITULAR (409). */
    @ExceptionHandler(RotationNotAllowedException.class)
    public ProblemDetail handleRotationNotAllowed(RotationNotAllowedException ex) {
        return problem(HttpStatus.CONFLICT, "ROTATION_NOT_ALLOWED", ex.getMessage());
    }

    /** There is no journal for the date of step 1 (404). */
    @ExceptionHandler(JournalNotFoundException.class)
    public ProblemDetail handleJournalNotFound(JournalNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "JOURNAL_NOT_FOUND", ex.getMessage());
    }

    /** The journal is closed and step 1 is read only (409). */
    @ExceptionHandler(JournalNotEditableException.class)
    public ProblemDetail handleJournalNotEditable(JournalNotEditableException ex) {
        return problem(HttpStatus.CONFLICT, "JOURNAL_NOT_EDITABLE", ex.getMessage());
    }

    /** The reason of a novelty does not have 3 to 255 characters (400). */
    @ExceptionHandler(InvalidNoveltyReasonException.class)
    public ProblemDetail handleInvalidNoveltyReason(InvalidNoveltyReasonException ex) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_NOVELTY_REASON", ex.getMessage());
    }

    /** The novelty does not fit the staff of the day (400). */
    @ExceptionHandler(InvalidNoveltyException.class)
    public ProblemDetail handleInvalidNovelty(InvalidNoveltyException ex) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_NOVELTY", ex.getMessage());
    }

    /** The replacement or the other worker of a swap is the same worker (400). */
    @ExceptionHandler(SameWorkerNoveltyException.class)
    public ProblemDetail handleSameWorker(SameWorkerNoveltyException ex) {
        return problem(HttpStatus.BAD_REQUEST, "SAME_WORKER", ex.getMessage());
    }

    /** The replacement works a shift whose hours cross the shift to cover (409). */
    @ExceptionHandler(ShiftHoursOverlapException.class)
    public ProblemDetail handleShiftHoursOverlap(ShiftHoursOverlapException ex) {
        return problem(HttpStatus.CONFLICT, "SHIFT_HOURS_OVERLAP", ex.getMessage());
    }

    /** The replacement already works another shift that day (409). */
    @ExceptionHandler(WorkerAlreadyWorkingException.class)
    public ProblemDetail handleWorkerAlreadyWorking(WorkerAlreadyWorkingException ex) {
        return problem(HttpStatus.CONFLICT, "WORKER_ALREADY_WORKING", ex.getMessage());
    }

    /** A code of another role was not marked as exceptional coverage (409). */
    @ExceptionHandler(ExceptionalCoverageRequiredException.class)
    public ProblemDetail handleExceptionalCoverageRequired(ExceptionalCoverageRequiredException ex) {
        return problem(HttpStatus.CONFLICT, "EXCEPTIONAL_COVERAGE_REQUIRED", ex.getMessage());
    }

    /** A worker already takes part in another active novelty that day (409). */
    @ExceptionHandler(NoveltyConflictException.class)
    public ProblemDetail handleNoveltyConflict(NoveltyConflictException ex) {
        return problem(HttpStatus.CONFLICT, "NOVELTY_CONFLICT", ex.getMessage());
    }

    /** Nobody would work the day, so step 1 cannot be confirmed (409). */
    @ExceptionHandler(NoStaffToConfirmException.class)
    public ProblemDetail handleNoStaffToConfirm(NoStaffToConfirmException ex) {
        return problem(HttpStatus.CONFLICT, "NO_STAFF_TO_CONFIRM", ex.getMessage());
    }

    /** The novelty does not exist or belongs to another date (404). */
    @ExceptionHandler(NoveltyNotFoundException.class)
    public ProblemDetail handleNoveltyNotFound(NoveltyNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "NOVELTY_NOT_FOUND", ex.getMessage());
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
