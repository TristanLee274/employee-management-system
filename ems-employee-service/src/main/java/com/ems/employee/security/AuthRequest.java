package com.ems.employee.security;

/**
 * Request DTO for the login endpoint (POST /api/auth/login).
 *
 * <p>
 * Java 21 Record — immutable, zero-boilerplate DTO. The record automatically
 * generates constructor, getters, equals(), hashCode(), and toString().
 * </p>
 *
 * @param username the login username
 * @param password the plaintext password (will be matched against BCrypt hash)
 */
public record AuthRequest(
        String username,
        String password) {
}
