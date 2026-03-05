package com.java.practice.ems.exception;

import com.java.practice.ems.common.ErrorResponse;
import com.java.practice.ems.employee.exception.EmployeeNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ Global Exception Handler ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 * Centralized exception handling across all REST controllers.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EmployeeNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(ErrorResponse.of(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        f -> Objects.requireNonNullElse(f.getDefaultMessage(), "Invalid")));
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        400,
                        "Bad Request",
                        "Validation failed",
                        errors,
                        Instant.now()));
    }
}
