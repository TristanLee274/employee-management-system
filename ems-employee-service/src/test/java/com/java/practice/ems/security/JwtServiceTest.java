package com.java.practice.ems.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ JwtService Tests — JWT Token Lifecycle in Isolation ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * <h2>WHAT WE TEST</h2>
 *
 * <p>
 * The JwtService is the core cryptographic component of our authentication
 * system. These tests verify the complete JWT lifecycle:
 * </p>
 * <ul>
 * <li><strong>Generation:</strong> Access tokens and refresh tokens are created
 * with correct claims (sub, roles, iat, exp).</li>
 * <li><strong>Validation:</strong> Valid tokens pass; expired, tampered, or
 * mismatched tokens are rejected.</li>
 * <li><strong>Extraction:</strong> Username, expiration, and custom claims
 * (roles) are correctly parsed from the token.</li>
 * </ul>
 *
 * <h2>TESTING APPROACH</h2>
 *
 * <p>
 * JwtService is a pure function wrapper around jjwt — no Spring context needed,
 * no mocks needed. We manually instantiate the service and set its fields via
 * reflection (since @Value injection won't happen outside Spring).
 * </p>
 */
@DisplayName("JwtService — JWT Token Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails adminUser;
    private UserDetails viewerUser;

    // Test secret key — must be at least 256 bits (32 bytes) for HMAC-SHA256
    private static final String TEST_SECRET = "my-super-secret-key-that-must-be-at-least-256-bits-long-for-HS256";
    private static final long ACCESS_TOKEN_EXPIRY_MS = 86400000L; // 24 hours
    private static final long REFRESH_TOKEN_EXPIRY_MS = 604800000L; // 7 days

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtService();

        // Inject configuration values via reflection (normally done by Spring @Value)
        setField(jwtService, "secretKeyString", TEST_SECRET);
        setField(jwtService, "jwtExpirationMs", ACCESS_TOKEN_EXPIRY_MS);
        setField(jwtService, "refreshExpirationMs", REFRESH_TOKEN_EXPIRY_MS);

        // Trigger @PostConstruct manually to initialize the signing key
        jwtService.init();

        // Create test UserDetails objects
        adminUser = User.builder()
                .username("admin")
                .password("encoded-password")
                .authorities(List.of(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_HR"),
                        new SimpleGrantedAuthority("ROLE_VIEWER")))
                .build();

        viewerUser = User.builder()
                .username("viewer")
                .password("encoded-password")
                .authorities(List.of(
                        new SimpleGrantedAuthority("ROLE_VIEWER")))
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TOKEN GENERATION
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Token Generation")
    class TokenGenerationTests {

        @Test
        @DisplayName("should generate non-null, non-empty access token")
        void should_GenerateNonEmptyAccessToken() {
            // WHEN
            String token = jwtService.generateToken(adminUser);

            // THEN
            assertThat(token).isNotNull().isNotBlank();
            // JWT format: header.payload.signature (3 parts separated by dots)
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("should generate non-null, non-empty refresh token")
        void should_GenerateNonEmptyRefreshToken() {
            String token = jwtService.generateRefreshToken(adminUser);
            assertThat(token).isNotNull().isNotBlank();
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("should generate different tokens for access vs refresh")
        void should_GenerateDifferentTokens_ForAccessVsRefresh() {
            String accessToken = jwtService.generateToken(adminUser);
            String refreshToken = jwtService.generateRefreshToken(adminUser);
            assertThat(accessToken).isNotEqualTo(refreshToken);
        }

        @Test
        @DisplayName("should generate different tokens for different users")
        void should_GenerateDifferentTokens_ForDifferentUsers() {
            String adminToken = jwtService.generateToken(adminUser);
            String viewerToken = jwtService.generateToken(viewerUser);
            assertThat(adminToken).isNotEqualTo(viewerToken);
        }

        @Test
        @DisplayName("should embed username as 'sub' claim in the token")
        void should_EmbedUsernameAsSubjectClaim() {
            String token = jwtService.generateToken(adminUser);
            String username = jwtService.extractUsername(token);
            assertThat(username).isEqualTo("admin");
        }

        @Test
        @DisplayName("should embed roles in the token claims")
        void should_EmbedRolesInTokenClaims() {
            String token = jwtService.generateToken(adminUser);

            // Extract the roles claim directly
            @SuppressWarnings("unchecked")
            List<String> roles = jwtService.extractClaim(token,
                    claims -> claims.get("roles", List.class));

            assertThat(roles)
                    .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_HR", "ROLE_VIEWER");
        }

        @Test
        @DisplayName("should set expiration in the future for access token")
        void should_SetFutureExpiration_ForAccessToken() {
            String token = jwtService.generateToken(adminUser);
            Date expiration = jwtService.extractExpiration(token);

            assertThat(expiration).isAfter(new Date());
            // Expiration should be roughly 24 hours from now (allowing 5s tolerance)
            long expectedExpiry = System.currentTimeMillis() + ACCESS_TOKEN_EXPIRY_MS;
            assertThat(expiration.getTime()).isCloseTo(expectedExpiry, within(5000L));
        }

        @Test
        @DisplayName("should set longer expiration for refresh token than access token")
        void should_SetLongerExpiration_ForRefreshToken() {
            String accessToken = jwtService.generateToken(adminUser);
            String refreshToken = jwtService.generateRefreshToken(adminUser);

            Date accessExp = jwtService.extractExpiration(accessToken);
            Date refreshExp = jwtService.extractExpiration(refreshToken);

            assertThat(refreshExp).isAfter(accessExp);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TOKEN VALIDATION
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Token Validation")
    class TokenValidationTests {

        @Test
        @DisplayName("should return true for valid token with matching user")
        void should_ReturnTrue_When_TokenIsValidAndUserMatches() {
            String token = jwtService.generateToken(adminUser);
            assertThat(jwtService.isTokenValid(token, adminUser)).isTrue();
        }

        @Test
        @DisplayName("should return false when token username doesn't match user")
        void should_ReturnFalse_When_UsernameMismatch() {
            String token = jwtService.generateToken(adminUser);
            // Validate against a DIFFERENT user
            assertThat(jwtService.isTokenValid(token, viewerUser)).isFalse();
        }

        @Test
        @DisplayName("should return false for expired token")
        void should_ReturnFalse_When_TokenIsExpired() throws Exception {
            // Create a JwtService with a very short expiration (1ms)
            JwtService shortLivedService = new JwtService();
            setField(shortLivedService, "secretKeyString", TEST_SECRET);
            setField(shortLivedService, "jwtExpirationMs", 1L); // 1ms
            setField(shortLivedService, "refreshExpirationMs", 1L);
            shortLivedService.init();

            String token = shortLivedService.generateToken(adminUser);

            // Wait for the token to expire
            Thread.sleep(50);

            assertThat(shortLivedService.isTokenValid(token, adminUser)).isFalse();
        }

        @Test
        @DisplayName("should return false for tampered token")
        void should_ReturnFalse_When_TokenIsTampered() {
            String token = jwtService.generateToken(adminUser);
            // Tamper with the payload (change a character in the middle)
            String tamperedToken = token.substring(0, token.length() / 2) + "X"
                    + token.substring(token.length() / 2 + 1);

            assertThat(jwtService.isTokenValid(tamperedToken, adminUser)).isFalse();
        }

        @Test
        @DisplayName("should return false for completely invalid token string")
        void should_ReturnFalse_When_TokenIsGarbage() {
            assertThat(jwtService.isTokenValid("not.a.jwt", adminUser)).isFalse();
        }

        @Test
        @DisplayName("should validate refresh token the same way as access token")
        void should_ValidateRefreshToken() {
            String refreshToken = jwtService.generateRefreshToken(adminUser);
            assertThat(jwtService.isTokenValid(refreshToken, adminUser)).isTrue();
        }

        @Test
        @DisplayName("should return false for token signed with different key")
        void should_ReturnFalse_When_SignedWithDifferentKey() throws Exception {
            // Create another JwtService with a DIFFERENT secret key
            JwtService otherService = new JwtService();
            setField(otherService, "secretKeyString",
                    "another-secret-key-must-also-be-at-least-256-bits-long-xxxxxxxxxxxx");
            setField(otherService, "jwtExpirationMs", ACCESS_TOKEN_EXPIRY_MS);
            setField(otherService, "refreshExpirationMs", REFRESH_TOKEN_EXPIRY_MS);
            otherService.init();

            String tokenFromOtherService = otherService.generateToken(adminUser);

            // Our jwtService should reject a token signed by a different key
            assertThat(jwtService.isTokenValid(tokenFromOtherService, adminUser)).isFalse();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CLAIMS EXTRACTION
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Claims Extraction")
    class ClaimsExtractionTests {

        @Test
        @DisplayName("should extract username from token")
        void should_ExtractUsername() {
            String token = jwtService.generateToken(viewerUser);
            assertThat(jwtService.extractUsername(token)).isEqualTo("viewer");
        }

        @Test
        @DisplayName("should extract expiration date from token")
        void should_ExtractExpiration() {
            String token = jwtService.generateToken(adminUser);
            Date expiration = jwtService.extractExpiration(token);
            assertThat(expiration).isNotNull().isAfter(new Date());
        }

        @Test
        @DisplayName("should extract custom claim using extractClaim()")
        void should_ExtractCustomClaim() {
            String token = jwtService.generateToken(adminUser);

            // Use the generic extractClaim with a lambda
            String subject = jwtService.extractClaim(token, Claims::getSubject);
            assertThat(subject).isEqualTo("admin");
        }

        @ParameterizedTest(name = "user ''{0}'' → extractable username")
        @ValueSource(strings = { "admin", "viewer" })
        @DisplayName("should extract correct username for various users")
        void should_ExtractCorrectUsername_ForVariousUsers(String username) {
            UserDetails user = User.builder()
                    .username(username)
                    .password("pass")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_VIEWER")))
                    .build();

            String token = jwtService.generateToken(user);
            assertThat(jwtService.extractUsername(token)).isEqualTo(username);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // AUTH RESPONSE RECORD TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("AuthResponse Record")
    class AuthResponseTests {

        @Test
        @DisplayName("should create AuthResponse with 'Bearer' tokenType via factory method")
        void should_CreateAuthResponse_WithBearerType() {
            AuthResponse response = AuthResponse.of(
                    "access-token", "refresh-token", 86400,
                    "admin", List.of("ROLE_ADMIN"));

            assertThat(response.accessToken()).isEqualTo("access-token");
            assertThat(response.refreshToken()).isEqualTo("refresh-token");
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.expiresIn()).isEqualTo(86400);
            assertThat(response.username()).isEqualTo("admin");
            assertThat(response.roles()).containsExactly("ROLE_ADMIN");
        }

        @Test
        @DisplayName("should support value-based equality (Java 21 Record)")
        void should_SupportValueEquality() {
            AuthResponse r1 = AuthResponse.of("a", "b", 100, "user", List.of("ROLE_VIEWER"));
            AuthResponse r2 = AuthResponse.of("a", "b", 100, "user", List.of("ROLE_VIEWER"));
            assertThat(r1).isEqualTo(r2);
            assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // AUTH REQUEST RECORD TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("AuthRequest Record")
    class AuthRequestTests {

        @Test
        @DisplayName("should store username and password correctly")
        void should_StoreCredentials() {
            AuthRequest request = new AuthRequest("admin", "secret123");
            assertThat(request.username()).isEqualTo("admin");
            assertThat(request.password()).isEqualTo("secret123");
        }

        @Test
        @DisplayName("should support value-based equality")
        void should_SupportValueEquality() {
            AuthRequest r1 = new AuthRequest("user", "pass");
            AuthRequest r2 = new AuthRequest("user", "pass");
            assertThat(r1).isEqualTo(r2);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITY: Reflection helper for setting private fields
    // ═══════════════════════════════════════════════════════════════════════════

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
