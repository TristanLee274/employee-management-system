package com.ems.employee.security;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Authentication controller handling login and token operations.
 *
 * <p>
 * This is a PUBLIC endpoint — no JWT required to access it.
 * See SecurityConfig: {@code .requestMatchers("/api/auth/**").permitAll()}
 * </p>
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Login and token management endpoints")
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * Authenticates a user and returns JWT tokens.
     *
     * <p>
     * Flow:
     * </p>
     * <ol>
     * <li>Client sends username + password</li>
     * <li>AuthenticationManager delegates to DaoAuthenticationProvider</li>
     * <li>Provider loads UserDetails + verifies BCrypt password</li>
     * <li>If valid → generate JWT access + refresh tokens</li>
     * <li>Return tokens to client</li>
     * </ol>
     *
     * @param request the login credentials
     * @return JWT tokens and user metadata
     */
    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Validates credentials and returns JWT access and refresh tokens")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        log.info("Login attempt for user='{}'", request.username());

        try {
            // Delegate authentication to Spring Security's AuthenticationManager
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()));

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // Generate JWT tokens
            String accessToken = jwtService.generateToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            // Extract roles for the response
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            log.info("Login successful for user='{}', roles={}", request.username(), roles);

            return ResponseEntity.ok(
                    AuthResponse.of(accessToken, refreshToken, 86400, request.username(), roles));

        } catch (BadCredentialsException e) {
            log.warn("Login failed for user='{}': invalid credentials", request.username());
            return ResponseEntity.status(401).build();
        }
    }
}
