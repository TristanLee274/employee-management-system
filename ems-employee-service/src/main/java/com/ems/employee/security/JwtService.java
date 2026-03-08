package com.ems.employee.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ JwtService — JWT Token Generation, Validation & Claims Extraction ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * <h2>JWT TOKEN ANATOMY</h2>
 *
 * <p>
 * A JWT consists of three Base64-encoded parts separated by dots:
 * </p>
 * 
 * <pre>
 *   eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiJ9.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV
 *   ─────────────────────  ───────────────────────  ─────────────────────────────────────
 *          HEADER                  PAYLOAD                     SIGNATURE
 *       (algorithm)         (claims: sub, roles,           (HMAC-SHA256 of
 *                            exp, iat)                     header + payload
 *                                                          using secret key)
 * </pre>
 *
 * <p>
 * <strong>Security guarantee:</strong> If ANY byte of the header or payload is
 * tampered with, the signature verification fails → token rejected.
 * </p>
 *
 * <h2>WHY STATELESS JWT OVER SERVER-SIDE SESSIONS</h2>
 *
 * <p>
 * The JWT carries ALL authentication state within the token itself:
 * </p>
 * <ul>
 * <li>{@code sub} (subject): the username or user ID</li>
 * <li>{@code roles}: the user's granted authorities (ROLE_ADMIN, ROLE_HR,
 * etc.)</li>
 * <li>{@code iat} (issued at): when the token was created</li>
 * <li>{@code exp} (expiration): when the token becomes invalid</li>
 * </ul>
 *
 * <p>
 * No database lookup required to validate the token — just verify the signature
 * and check expiry. This is O(1) authentication, not O(n) session lookup.
 * </p>
 */
@Service
@Slf4j
public class JwtService {

    // ── Configuration from application.yml ──────────────────────────────────
    // These values are injected from environment variables or YAML config,
    // following the 12-Factor App externalized configuration principle.

    @Value("${jwt.secret:my-super-secret-key-that-must-be-at-least-256-bits-long-for-HS256}")
    private String secretKeyString;

    @Value("${jwt.expiration:86400000}") // Default: 24 hours in milliseconds
    private long jwtExpirationMs;

    @Value("${jwt.refresh-expiration:604800000}") // Default: 7 days
    private long refreshExpirationMs;

    private SecretKey signingKey;

    /**
     * Initialize the HMAC signing key after Spring injects the properties.
     * <p>
     * Using {@code @PostConstruct} ensures the key is ready before any
     * request processing begins.
     * </p>
     */
    @PostConstruct
    public void init() {
        // HMAC-SHA256 requires a key of at least 256 bits (32 bytes)
        this.signingKey = Keys.hmacShaKeyFor(
                secretKeyString.getBytes(StandardCharsets.UTF_8));
        log.info("JWT signing key initialized (algorithm: HS256)");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TOKEN GENERATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Generates a JWT access token for an authenticated user.
     *
     * <p>
     * The token contains:
     * </p>
     * <ul>
     * <li>{@code sub}: username</li>
     * <li>{@code roles}: list of granted authorities (e.g., ["ROLE_HR",
     * "ROLE_ADMIN"])</li>
     * <li>{@code iat}: current timestamp</li>
     * <li>{@code exp}: iat + configured expiration (default 24h)</li>
     * </ul>
     *
     * @param userDetails the authenticated user's details (from UserDetailsService)
     * @return a signed JWT string
     */
    public String generateToken(UserDetails userDetails) {
        return buildToken(userDetails, jwtExpirationMs);
    }

    /**
     * Generates a refresh token with a longer expiration (default 7 days).
     *
     * <p>
     * Refresh tokens allow the client to obtain new access tokens without
     * re-entering credentials, improving UX while maintaining security.
     * </p>
     */
    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(userDetails, refreshExpirationMs);
    }

    /**
     * Builds a JWT token with the specified expiration duration.
     *
     * <p>
     * This is a private helper that both {@link #generateToken} and
     * {@link #generateRefreshToken} delegate to — DRY principle.
     * </p>
     */
    private String buildToken(UserDetails userDetails, long expirationMs) {
        // Extract role names from GrantedAuthority objects
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList(); // Java 21: .toList() returns an unmodifiable list

        long now = System.currentTimeMillis();

        return Jwts.builder()
                .subject(userDetails.getUsername()) // WHO this token represents
                .claim("roles", roles) // WHAT they can do
                .issuedAt(new Date(now)) // WHEN the token was issued
                .expiration(new Date(now + expirationMs)) // WHEN the token expires
                .signWith(signingKey) // SIGN with HMAC-SHA256
                .compact(); // Build the final JWT string
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TOKEN VALIDATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Validates a JWT token against the expected user.
     *
     * <p>
     * Validation checks (all must pass):
     * </p>
     * <ol>
     * <li>Signature is valid (token was not tampered with)</li>
     * <li>Token has not expired</li>
     * <li>Username in token matches the expected UserDetails</li>
     * </ol>
     *
     * @param token       the JWT string to validate
     * @param userDetails the user to validate against
     * @return true if the token is valid for this user
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException e) {
            // JwtException covers: ExpiredJwtException, MalformedJwtException,
            // SignatureException, UnsupportedJwtException
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CLAIMS EXTRACTION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Extracts the username (subject claim) from a JWT token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the expiration date from a JWT token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extracts any claim from a JWT token using a resolver function.
     *
     * <p>
     * This is a generic extraction method using a {@link Function} parameter,
     * following the Strategy Pattern at the function level — the caller decides
     * WHICH claim to extract by passing a lambda or method reference.
     * </p>
     *
     * @param token          the JWT string
     * @param claimsResolver a function to extract the desired claim
     * @param <T>            the return type of the claim
     * @return the extracted claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parses and verifies the JWT, returning all claims.
     *
     * <p>
     * This is where the actual cryptographic validation happens:
     * </p>
     * <ol>
     * <li>Base64-decode header and payload</li>
     * <li>Recompute HMAC-SHA256 signature using our secret key</li>
     * <li>Compare computed signature with the token's signature</li>
     * <li>Check expiration against current time</li>
     * </ol>
     *
     * <p>
     * If any check fails, jjwt throws a {@link JwtException}.
     * </p>
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey) // Set the key for signature verification
                .build()
                .parseSignedClaims(token) // Parse AND verify in one step
                .getPayload(); // Return the claims payload
    }

    /**
     * Checks whether a JWT token has expired.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}
