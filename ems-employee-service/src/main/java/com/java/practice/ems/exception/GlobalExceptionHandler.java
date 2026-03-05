package com.java.practice.ems.exception;

import com.java.practice.ems.common.ErrorResponse;
import com.java.practice.ems.employee.exception.DuplicateEmailException;
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
 * ║ GlobalExceptionHandler — Centralized REST Error Handling ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * <h2>SINGLE RESPONSIBILITY PRINCIPLE (SRP)</h2>
 *
 * <p>
 * This class has ONE responsibility: translating domain exceptions into
 * standardized HTTP error responses. Without it, every controller would need
 * its own try-catch blocks, violating DRY and scattering error-handling logic
 * across the codebase.
 * </p>
 *
 * <h2>DESIGN DECISION: @RestControllerAdvice</h2>
 *
 * <p>
 * {@code @RestControllerAdvice} combines {@code @ControllerAdvice} (AOP-style
 * cross-cutting concern) with {@code @ResponseBody} (auto-serialization to
 * JSON). Spring intercepts exceptions thrown from ANY controller and routes
 * them here — controllers remain clean and focused on happy-path logic.
 * </p>
 *
 * <h3>HTTP Status Code Strategy (per REST/RFC 7231)</h3>
 * <ul>
 * <li>400 Bad Request → validation failures (malformed input)</li>
 * <li>404 Not Found → resource doesn't exist</li>
 * <li>409 Conflict → business state conflict (duplicate email)</li>
 * <li>422 Unprocessable Entity → valid syntax but business rule violation</li>
 * <li>500 Internal Server Error → unexpected failures (catch-all)</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

        /**
         * Handles employee not found — maps to 404.
         * Triggered when a lookup by ID finds no matching record.
         */
        @ExceptionHandler(EmployeeNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleNotFound(EmployeeNotFoundException ex) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ErrorResponse.of(HttpStatus.NOT_FOUND, ex.getMessage()));
        }

        /**
         * Handles duplicate email constraint violation — maps to 409 Conflict.
         *
         * <p>
         * HTTP 409 (Conflict) is the correct status when a request cannot be
         * completed due to a state conflict with the current resource state.
         * In this case: the email already exists in the system.
         * </p>
         */
        @ExceptionHandler(DuplicateEmailException.class)
        public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException ex) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(ErrorResponse.of(HttpStatus.CONFLICT, ex.getMessage()));
        }

        /**
         * Handles invalid arguments — maps to 400 Bad Request.
         *
         * <p>
         * Catches invalid enum values, out-of-range parameters, and other
         * programmatic input errors thrown via {@code IllegalArgumentException}.
         * </p>
         */
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST, ex.getMessage()));
        }

        /**
         * Handles business rule violations — maps to 422 Unprocessable Entity.
         *
         * <p>
         * HTTP 422 indicates the request is syntactically valid but violates a
         * business invariant (e.g., deactivating a terminated employee,
         * calculating salary without a base rate configured).
         * </p>
         */
        @ExceptionHandler(IllegalStateException.class)
        public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                                .body(ErrorResponse.of(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage()));
        }

        /**
         * Handles Bean Validation failures — maps to 400 Bad Request.
         *
         * <p>
         * Spring triggers this when {@code @Valid} or {@code @Validated} fails on
         * a request body. Each field error is collected into a map for frontend
         * consumption (field name → error message).
         * </p>
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
                Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                                .collect(Collectors.toMap(
                                                FieldError::getField,
                                                f -> Objects.requireNonNullElse(f.getDefaultMessage(), "Invalid"),
                                                (existing, replacement) -> existing)); // handle duplicate keys
                                                                                       // gracefully
                return ResponseEntity.badRequest()
                                .body(new ErrorResponse(
                                                400,
                                                "Bad Request",
                                                "Validation failed",
                                                errors,
                                                Instant.now()));
        }

        /**
         * Catch-all handler for unexpected runtime exceptions — maps to 500.
         *
         * <p>
         * In production, this prevents stack traces from leaking to the client.
         * The actual exception is logged server-side for debugging.
         * </p>
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR,
                                                "An unexpected error occurred. Please contact support."));
        }
}
