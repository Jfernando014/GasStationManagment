package com.edu.unicauca.gasstation.backend.backup.api;

import com.edu.unicauca.gasstation.backend.backup.exception.BackupException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Turns backup errors into RFC 9457 problem details.
 *
 * @author javiersolanop777
 */
@RestControllerAdvice(basePackageClasses = BackupController.class)
public class BackupExceptionHandler {

    @ExceptionHandler(BackupException.class)
    public ProblemDetail handleBackupException(BackupException prmException) {
        HttpStatus objStatus = switch (prmException.getReason()) {
            case BUSY -> HttpStatus.CONFLICT;
            case RESTORE_DISABLED -> HttpStatus.FORBIDDEN;
            case CONFIRMATION_REQUIRED, INVALID_FILE -> HttpStatus.BAD_REQUEST;
            case FILE_TOO_LARGE -> HttpStatus.PAYLOAD_TOO_LARGE;
            case DUMP_FAILED, RESTORE_FAILED, MIGRATION_FAILED, IO_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return problem(objStatus, prmException.getReason().name(), prmException.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUploadSize(MaxUploadSizeExceededException prmException) {
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, BackupException.Reason.FILE_TOO_LARGE.name(),
                "The backup is larger than the allowed limit");
    }

    private ProblemDetail problem(HttpStatus prmStatus, String prmCode, String prmDetail) {
        ProblemDetail objProblem = ProblemDetail.forStatusAndDetail(prmStatus, prmDetail);
        objProblem.setTitle(prmStatus.getReasonPhrase());
        objProblem.setProperty("code", prmCode);
        return objProblem;
    }
}
