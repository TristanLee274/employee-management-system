package com.java.practice.ems.security;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ AuthController Tests — Login Endpoint Unit Tests ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * <h2>TESTING THE AUTH CONTROLLER IN ISOLATION</h2>
 *
 * <p>
 * We mock the {@link AuthenticationManager} and {@link JwtService} to test
 * the controller's orchestration logic WITHOUT requiring Spring Security
 * infrastructure or a running application context.
 * </p>
 *
 * <p>
 * Tests verify:
 * </p>
 * <ul>
 * <li>Successful login returns 200 with JWT tokens and user metadata</li>
 * <li>Invalid credentials return 401 Unauthorized</li>
 * <li>Correct roles are extracted from authentication principal</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController — Unit Tests")
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthController authController;

    // Test fixtures
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin123";
    private static final String ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiJ9.access.signature";
    private static final String REFRESH_TOKEN = "eyJhbGciOiJIUzI1NiJ9.refresh.signature";

    private UserDetails createAdminUserDetails() {
        return User.builder()
                .username(USERNAME)
                .password("$2a$10$encodedPassword")
                .authorities(List.of(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_HR"),
                        new SimpleGrantedAuthority("ROLE_VIEWER")))
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SUCCESSFUL LOGIN
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Successful Login")
    class SuccessfulLoginTests {

        @Test
        @DisplayName("should return 200 with JWT tokens when credentials are valid")
        void should_Return200WithTokens_When_CredentialsValid() {
            // ── GIVEN ─────────────────────────────────────────────────────────
            AuthRequest request = new AuthRequest(USERNAME, PASSWORD);
            UserDetails userDetails = createAdminUserDetails();
            Authentication authentication = mock(Authentication.class);

            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .willReturn(authentication);
            given(authentication.getPrincipal()).willReturn(userDetails);
            given(jwtService.generateToken(userDetails)).willReturn(ACCESS_TOKEN);
            given(jwtService.generateRefreshToken(userDetails)).willReturn(REFRESH_TOKEN);

            // ── WHEN ──────────────────────────────────────────────────────────
            ResponseEntity<AuthResponse> response = authController.login(request);

            // ── THEN ──────────────────────────────────────────────────────────
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isNotNull();

            AuthResponse body = response.getBody();
            assertThat(body.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(body.refreshToken()).isEqualTo(REFRESH_TOKEN);
            assertThat(body.tokenType()).isEqualTo("Bearer");
            assertThat(body.expiresIn()).isEqualTo(86400);
            assertThat(body.username()).isEqualTo(USERNAME);
            assertThat(body.roles()).containsExactlyInAnyOrder(
                    "ROLE_ADMIN", "ROLE_HR", "ROLE_VIEWER");
        }

        @Test
        @DisplayName("should extract roles from authenticated principal")
        void should_ExtractRoles_FromAuthenticatedPrincipal() {
            // ── GIVEN ─────────────────────────────────────────────────────────
            UserDetails viewerUser = User.builder()
                    .username("viewer")
                    .password("encoded")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_VIEWER")))
                    .build();
            Authentication authentication = mock(Authentication.class);

            given(authenticationManager.authenticate(any())).willReturn(authentication);
            given(authentication.getPrincipal()).willReturn(viewerUser);
            given(jwtService.generateToken(viewerUser)).willReturn("token");
            given(jwtService.generateRefreshToken(viewerUser)).willReturn("refresh");

            // ── WHEN ──────────────────────────────────────────────────────────
            ResponseEntity<AuthResponse> response = authController.login(
                    new AuthRequest("viewer", "viewer123"));

            // ── THEN ──────────────────────────────────────────────────────────
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().roles()).containsExactly("ROLE_VIEWER");
            assertThat(response.getBody().username()).isEqualTo("viewer");
        }

        @Test
        @DisplayName("should call AuthenticationManager with correct credentials")
        void should_DelegateToAuthenticationManager() {
            // ── GIVEN ─────────────────────────────────────────────────────────
            AuthRequest request = new AuthRequest(USERNAME, PASSWORD);
            Authentication authentication = mock(Authentication.class);
            UserDetails userDetails = createAdminUserDetails();

            given(authenticationManager.authenticate(any())).willReturn(authentication);
            given(authentication.getPrincipal()).willReturn(userDetails);
            given(jwtService.generateToken(any())).willReturn("token");
            given(jwtService.generateRefreshToken(any())).willReturn("refresh");

            // ── WHEN ──────────────────────────────────────────────────────────
            authController.login(request);

            // ── THEN ──────────────────────────────────────────────────────────
            ArgumentCaptor<UsernamePasswordAuthenticationToken> captor = ArgumentCaptor
                    .forClass(UsernamePasswordAuthenticationToken.class);
            then(authenticationManager).should().authenticate(captor.capture());

            UsernamePasswordAuthenticationToken captured = captor.getValue();
            assertThat(captured.getPrincipal()).isEqualTo(USERNAME);
            assertThat(captured.getCredentials()).isEqualTo(PASSWORD);
        }

        @Test
        @DisplayName("should call JwtService to generate both tokens")
        void should_GenerateBothTokens() {
            // ── GIVEN ─────────────────────────────────────────────────────────
            UserDetails userDetails = createAdminUserDetails();
            Authentication authentication = mock(Authentication.class);

            given(authenticationManager.authenticate(any())).willReturn(authentication);
            given(authentication.getPrincipal()).willReturn(userDetails);
            given(jwtService.generateToken(userDetails)).willReturn(ACCESS_TOKEN);
            given(jwtService.generateRefreshToken(userDetails)).willReturn(REFRESH_TOKEN);

            // ── WHEN ──────────────────────────────────────────────────────────
            authController.login(new AuthRequest(USERNAME, PASSWORD));

            // ── THEN ──────────────────────────────────────────────────────────
            then(jwtService).should().generateToken(userDetails);
            then(jwtService).should().generateRefreshToken(userDetails);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FAILED LOGIN
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Failed Login")
    class FailedLoginTests {

        @Test
        @DisplayName("should return 401 when credentials are invalid")
        void should_Return401_When_CredentialsInvalid() {
            // ── GIVEN ─────────────────────────────────────────────────────────
            AuthRequest request = new AuthRequest("wrong-user", "wrong-pass");
            given(authenticationManager.authenticate(any()))
                    .willThrow(new BadCredentialsException("Invalid credentials"));

            // ── WHEN ──────────────────────────────────────────────────────────
            ResponseEntity<AuthResponse> response = authController.login(request);

            // ── THEN ──────────────────────────────────────────────────────────
            assertThat(response.getStatusCode().value()).isEqualTo(401);
            assertThat(response.getBody()).isNull();
        }

        @Test
        @DisplayName("should NOT generate JWT tokens when authentication fails")
        void should_NotGenerateTokens_When_AuthenticationFails() {
            // ── GIVEN ─────────────────────────────────────────────────────────
            given(authenticationManager.authenticate(any()))
                    .willThrow(new BadCredentialsException("Bad credentials"));

            // ── WHEN ──────────────────────────────────────────────────────────
            authController.login(new AuthRequest("invalid", "invalid"));

            // ── THEN ──────────────────────────────────────────────────────────
            then(jwtService).shouldHaveNoInteractions();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // IN-MEMORY USER DETAILS SERVICE
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("InMemoryUserDetailsService")
    class InMemoryUserDetailsServiceTest {

        private InMemoryUserDetailsService userDetailsService;

        @BeforeEach
        void setUp() {
            // Use a real BCryptPasswordEncoder for testing the service
            userDetailsService = new InMemoryUserDetailsService(
                    new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder());
        }

        @Test
        @DisplayName("should return UserDetails for 'viewer' with ROLE_VIEWER")
        void should_ReturnViewerUser() {
            UserDetails user = userDetailsService.loadUserByUsername("viewer");
            assertThat(user.getUsername()).isEqualTo("viewer");
            assertThat(user.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_VIEWER");
        }

        @Test
        @DisplayName("should return UserDetails for 'hr' with ROLE_HR and ROLE_VIEWER")
        void should_ReturnHrUser() {
            UserDetails user = userDetailsService.loadUserByUsername("hr");
            assertThat(user.getUsername()).isEqualTo("hr");
            assertThat(user.getAuthorities())
                    .extracting("authority")
                    .containsExactlyInAnyOrder("ROLE_HR", "ROLE_VIEWER");
        }

        @Test
        @DisplayName("should return UserDetails for 'admin' with all three roles")
        void should_ReturnAdminUser() {
            UserDetails user = userDetailsService.loadUserByUsername("admin");
            assertThat(user.getUsername()).isEqualTo("admin");
            assertThat(user.getAuthorities())
                    .extracting("authority")
                    .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_HR", "ROLE_VIEWER");
        }

        @Test
        @DisplayName("should throw UsernameNotFoundException for unknown user")
        void should_ThrowException_ForUnknownUser() {
            assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown"))
                    .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("should encode passwords with BCrypt (non-null, non-empty)")
        void should_EncodedPasswordsWithBCrypt() {
            UserDetails user = userDetailsService.loadUserByUsername("admin");
            assertThat(user.getPassword()).isNotNull().isNotBlank();
            // BCrypt hashes start with $2a$ or $2b$
            assertThat(user.getPassword()).startsWith("$2a$");
        }
    }
}
