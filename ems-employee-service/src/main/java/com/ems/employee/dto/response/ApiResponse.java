package com.ems.employee.dto.response;

import java.time.Instant;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ ApiResponse — Standard API Format ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 * Uses Java 21 Records for immutability and concise definition.
 */
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        Instant timestamp) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Success", data, Instant.now());
    }
}
