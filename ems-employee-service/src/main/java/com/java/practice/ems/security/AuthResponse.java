package com.java.practice.ems.security;

import java.util.List;

/**
 * Response DTO returned after successful authentication.
 *
 * <p>
 * Contains the JWT access token, refresh token, and metadata the client
 * needs to manage the authentication lifecycle.
 * </p>
 *
 * @param accessToken  short-lived JWT for API authentication (default 24h)
 * @param refreshToken long-lived JWT for obtaining new access tokens (default
 *                     7d)
 * @param tokenType    always "Bearer" — indicates the authentication scheme
 * @param expiresIn    access token validity in seconds
 * @param username     the authenticated user's username
 * @param roles        the user's granted roles for client-side UI rendering
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        String username,
        List<String> roles) {
    /**
     * Factory method for creating a standard response.
     */
    public static AuthResponse of(String accessToken, String refreshToken,
            long expiresInSeconds, String username,
            List<String> roles) {
        return new AuthResponse(accessToken, refreshToken, "Bearer",
                expiresInSeconds, username, roles);
    }
}
