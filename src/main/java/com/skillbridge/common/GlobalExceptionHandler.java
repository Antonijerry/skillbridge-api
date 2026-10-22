package com.skillbridge.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** One error format for every failure: { "error": { "code", "message", "details?" } }. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApi(ApiException ex) {
        return body(ex.getStatus(), ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of("field", fe.getField(), "message", String.valueOf(fe.getDefaultMessage())))
                .toList();
        return body(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid input", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        return body(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
                "Malformed JSON, unknown enum value or invalid UUID in the request body", null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return body(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
                "Invalid value for '" + ex.getName() + "'", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return body(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Method not supported on this path", null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoRoute(NoResourceFoundException ex) {
        return body(HttpStatus.NOT_FOUND, "NOT_FOUND", "No such path", null);
    }

    /** Safety net: two simultaneous PENDING claims pass the service check, the database index stops the second. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleIntegrity(DataIntegrityViolationException ex) {
        String cause = String.valueOf(ex.getMostSpecificCause().getMessage());
        if (cause.contains("uq_assessment_one_pending")) {
            return body(HttpStatus.CONFLICT, "PENDING_CLAIM_EXISTS",
                    "A pending claim for this skill already exists. Revise or withdraw it first.", null);
        }
        if (cause.contains("uq_employees_email")) {
            return body(HttpStatus.CONFLICT, "EMAIL_TAKEN", "An account with this email already exists", null);
        }
        if (cause.contains("uq_skills_name_lower")) {
            return body(HttpStatus.CONFLICT, "SKILL_NAME_TAKEN", "A skill with this name already exists", null);
        }
        log.error("Unexpected data integrity violation", ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected error", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAny(Exception ex) {
        log.error("Unexpected error", ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected error", null);
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String code, String message, Object details) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", code);
        error.put("message", message);
        if (details != null) {
            error.put("details", details);
        }
        return ResponseEntity.status(status).body(Map.of("error", error));
    }
}
