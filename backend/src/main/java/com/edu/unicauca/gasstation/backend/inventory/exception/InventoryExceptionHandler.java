package com.edu.unicauca.gasstation.backend.inventory.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.edu.unicauca.gasstation.backend.inventory")
public class InventoryExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> firstMessageByField = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                firstMessageByField.putIfAbsent(error.getField(), error.getDefaultMessage()));

        List<Map<String, String>> fields = new ArrayList<>();
        firstMessageByField.forEach((field, message) -> {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("field", field);
            item.put("message", message);
            fields.add(item);
        });

        Map<String, Object> body = buildBody(HttpStatus.BAD_REQUEST, "Invalid data",
                "One or more fields failed validation", request);
        body.put("fields", fields);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Invalid data",
                "The request is invalid: Please check that the numerical values and fuel type are correct",
                request);
    }

    @ExceptionHandler(TankNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTankNotFound(
            TankNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Not found", ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateTankCodeException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateTankCode(
            DuplicateTankCodeException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Conflict",
                "The tank could not be saved: the data conflicts with existing records", request);
    }

    private ResponseEntity<Map<String, Object>> build(
            HttpStatus status, String error, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(buildBody(status, error, message, request));
    }

    private Map<String, Object> buildBody(
            HttpStatus status, String error, String message, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.put("path", request.getRequestURI());
        return body;
    }

    @ExceptionHandler(FuelPriceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleFuelPriceNotFound(
            FuelPriceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Not found", ex.getMessage(), request);
    }


    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleProductNotFound(
            ProductNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Not found", ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateProductCodeException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateProductCode(
            DuplicateProductCodeException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Invalid data",
                "The value of parameter '" + ex.getName() + "' is invalid", request);
    }

    @ExceptionHandler(InvalidGaugeTableException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidGaugeTable(
            InvalidGaugeTableException ex, HttpServletRequest request) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("field", ex.getField());
        item.put("message", ex.getDetail());
        Map<String, Object> body = buildBody(HttpStatus.BAD_REQUEST, "Invalid data",
                ex.getDetail() != null ? ex.getDetail() : ex.getMessage(), request);
        body.put("fields", List.of(item));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HeightExceedsGaugeException.class)
    public ResponseEntity<Map<String, Object>> handleHeightExceedsGauge(
            HeightExceedsGaugeException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Invalid data", ex.getMessage(), request);
    }

    @ExceptionHandler(GaugeTableNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleGaugeTableNotFound(
            GaugeTableNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Not found", ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateGaugeTableNameException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateGaugeTableName(
            DuplicateGaugeTableNameException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
    }

    @ExceptionHandler(TankWithoutGaugeTableException.class)
    public ResponseEntity<Map<String, Object>> handleTankWithoutGaugeTable(
            TankWithoutGaugeTableException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        List<Map<String, String>> fields = new ArrayList<>();
        ex.getConstraintViolations().forEach(violation -> {
            String path = violation.getPropertyPath().toString();
            Map<String, String> item = new LinkedHashMap<>();
            item.put("field", path.substring(path.lastIndexOf('.') + 1));
            item.put("message", violation.getMessage());
            fields.add(item);
        });
        Map<String, Object> body = buildBody(HttpStatus.BAD_REQUEST, "Invalid data",
                "One or more parameters failed validation", request);
        body.put("fields", fields);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Invalid data",
                "The required parameter is missing '" + ex.getParameterName() + "'", request);
    }
}