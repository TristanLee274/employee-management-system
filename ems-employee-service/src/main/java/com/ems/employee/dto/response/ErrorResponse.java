package com.ems.employee.dto.response;

import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ ErrorResponse — Standard Error Information Format ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 * Uses Java 21 Records for immutability and concise definition.
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        Map<String, String> details,
        Instant timestamp) {
    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(status.value(), status.getReasonPhrase(), message, Map.of(), Instant.now());
    }
}
