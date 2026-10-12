package com.edu.unicauca.gasstation.backend.sales.api;

import com.edu.unicauca.gasstation.backend.sales.exception.InvalidJournalFileException;
import com.edu.unicauca.gasstation.backend.sales.exception.JournalFileNotFoundException;
import com.edu.unicauca.gasstation.backend.sales.exception.JournalFileReadException;
import com.edu.unicauca.gasstation.backend.sales.exception.JournalNotFoundException;
import com.edu.unicauca.gasstation.backend.sales.exception.JournalWorkbookGenerationException;
import org.springframework.beans.TypeMismatchException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice(assignableTypes = JournalFileController.class)
public class JournalFileExceptionHandler {

    @ExceptionHandler(TypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(TypeMismatchException ex) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "El valor del parámetro no es válido");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "La solicitud contiene datos no válidos");
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(JournalFileExceptionHandler::toFieldError)
                .toList();
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMalformedRequest(HttpMessageNotReadableException ex) {
        return problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "El cuerpo de la solicitud no es válido");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ProblemDetail handleMissingFile(MissingServletRequestPartException ex) {
        return problem(HttpStatus.BAD_REQUEST, "MISSING_JOURNAL_FILE", "Debe adjuntar un archivo");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                "El tipo de contenido de la solicitud no es válido");
    }

    @ExceptionHandler(JournalNotFoundException.class)
    public ProblemDetail handleJournalNotFound(JournalNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "JOURNAL_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(JournalFileNotFoundException.class)
    public ProblemDetail handleJournalFileNotFound(JournalFileNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "JOURNAL_FILE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(InvalidJournalFileException.class)
    public ProblemDetail handleInvalidFile(InvalidJournalFileException ex) {
        HttpStatus status = ex.getReason() == InvalidJournalFileException.Reason.TOO_LARGE
                ? HttpStatus.PAYLOAD_TOO_LARGE
                : HttpStatus.BAD_REQUEST;
        String code = switch (ex.getReason()) {
            case EMPTY -> "EMPTY_JOURNAL_FILE";
            case INVALID_NAME -> "INVALID_JOURNAL_FILE_NAME";
            case UNSUPPORTED_TYPE -> "UNSUPPORTED_JOURNAL_FILE_TYPE";
            case TOO_LARGE -> "JOURNAL_FILE_TOO_LARGE";
        };
        return problem(status, code, ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, "JOURNAL_FILE_TOO_LARGE",
                "El archivo supera el límite de 25 MB");
    }

    @ExceptionHandler(JournalFileReadException.class)
    public ProblemDetail handleFileRead(JournalFileReadException ex) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_JOURNAL_FILE_CONTENT",
                "No fue posible leer el archivo enviado");
    }

    @ExceptionHandler(JournalWorkbookGenerationException.class)
    public ProblemDetail handleWorkbookGeneration(JournalWorkbookGenerationException ex) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "JOURNAL_WORKBOOK_GENERATION_FAILED",
                "No fue posible generar el archivo Excel de la jornada");
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