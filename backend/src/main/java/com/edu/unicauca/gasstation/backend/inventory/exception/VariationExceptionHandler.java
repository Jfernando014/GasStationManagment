package com.edu.unicauca.gasstation.backend.inventory.exception;

import com.edu.unicauca.gasstation.backend.inventory.api.MonthlyFuelVariationController;
import com.edu.unicauca.gasstation.backend.inventory.api.VariationAlertSettingController;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Errors of the monthly variation and its alert settings, with Spanish messages and the body of the module. It takes
 * precedence over {@link InventoryExceptionHandler}, whose generic messages are in English.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {VariationAlertSettingController.class, MonthlyFuelVariationController.class})
public class VariationExceptionHandler {

    private static final String BAD_REQUEST = "Invalid data";
    private static final String NOT_FOUND = "Not found";
    private static final String CONFLICT = "Conflict";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> firstMessageByField = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                firstMessageByField.putIfAbsent(error.getField(), error.getDefaultMessage()));
        List<Map<String, String>> fields = firstMessageByField.entrySet().stream().map(entry -> {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("field", entry.getKey());
            item.put("message", entry.getValue());
            return item;
        }).toList();
        Map<String, Object> body = body(HttpStatus.BAD_REQUEST, BAD_REQUEST,
                "Uno o más campos no son válidos", request);
        body.put("fields", fields);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, BAD_REQUEST,
                "La solicitud no es válida: revisa que los valores sean numéricos y que la fecha tenga el formato "
                        + "aaaa-mm-dd", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, BAD_REQUEST,
                "El valor del parámetro '" + ex.getName() + "' no es válido", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, BAD_REQUEST,
                "Falta el parámetro obligatorio '" + ex.getParameterName() + "'", request);
    }

    @ExceptionHandler({InvalidVariationAlertSettingException.class, InvalidVariationMonthException.class})
    public ResponseEntity<Map<String, Object>> handleInvalid(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(VariationAlertSettingNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            VariationAlertSettingNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler({VariationAlertSettingAlreadyExistsException.class,
            ProtectedVariationAlertSettingException.class})
    public ResponseEntity<Map<String, Object>> handleConflict(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, CONFLICT, ex.getMessage(), request);
    }

    /** Two requests saving the same validity date at once reach the database constraint. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, CONFLICT,
                "No se pudo guardar: los datos entran en conflicto con una configuración existente", request);
    }

    private ResponseEntity<Map<String, Object>> build(
            HttpStatus status, String error, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(body(status, error, message, request));
    }

    private Map<String, Object> body(HttpStatus status, String error, String message, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.put("path", request.getRequestURI());
        return body;
    }
}
