package com.java.practice.ems.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ JwtAuthenticationFilter — Custom Security Filter for JWT Validation ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * <h2>SPRING SECURITY FILTER CHAIN — Position of This Filter</h2>
 *
 * <p>
 * This filter extends {@link OncePerRequestFilter}, which guarantees it
 * executes
 * <strong>exactly once per request</strong> (even if the request is forwarded
 * or
 * dispatched internally). It is registered in the filter chain BEFORE
 * {@code UsernamePasswordAuthenticationFilter} (see SecurityConfig):
 * </p>
 *
 * <pre>
 *   Incoming HTTP Request
 *       │
 *       ├─ CorsFilter
 *       ├─ CsrfFilter (disabled for stateless APIs)
 *       ├─ ★ JwtAuthenticationFilter ← THIS CLASS
 *       │      │
 *       │      ├─ Extract "Authorization: Bearer xxx" header
 *       │      ├─ Parse and validate JWT token
 *       │      ├─ Load UserDetails from UserDetailsService
 *       │      ├─ If valid → set SecurityContextHolder authentication
 *       │      └─ If invalid → do nothing (let downstream filters handle)
 *       │
 *       ├─ UsernamePasswordAuthenticationFilter (skipped — already authenticated)
 *       ├─ AuthorizationFilter (checks roles against URL rules)
 *       └─ DispatcherServlet → Controller
 * </pre>
 *
 * <p>
 * <strong>Key Security Principle:</strong> This filter sits BEFORE the
 * controller.
 * If the JWT is missing or invalid, the SecurityContext remains empty, and the
 * AuthorizationFilter will return HTTP 401 Unauthorized — the controller never
 * executes.
 * </p>
 *
 * <h2>WHY OncePerRequestFilter?</h2>
 * <p>
 * In a Spring MVC application, a request might be dispatched multiple times
 * (e.g., forward from /login to /login?error). {@code OncePerRequestFilter}
 * prevents double-processing of the JWT, which could cause subtle bugs like
 * re-validating an already-consumed token or double-logging.
 * </p>
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    // ── Constructor injection (Dependency Inversion Principle) ────────────────
    // This filter depends on two interfaces:
    // • JwtService: handles token parsing/validation (our custom service)
    // • UserDetailsService: loads user details from the database
    // → We inject the interface, not the concrete class. In tests, we can inject
    // a mock UserDetailsService that returns test users without hitting the DB.
    public JwtAuthenticationFilter(JwtService jwtService,
            UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Core filter logic: extract JWT, validate, and set authentication context.
     *
     * <p>
     * This method is called for EVERY HTTP request that passes through the
     * security filter chain. The flow:
     * </p>
     *
     * <ol>
     * <li>Extract the Authorization header</li>
     * <li>Check if it starts with "Bearer " — if not, skip (allow chain to
     * continue)</li>
     * <li>Extract the JWT token (remove "Bearer " prefix)</li>
     * <li>Parse the username from the token</li>
     * <li>If no existing authentication in SecurityContext:
     * <ul>
     * <li>Load UserDetails from UserDetailsService</li>
     * <li>Validate the token against the UserDetails</li>
     * <li>If valid: create an Authentication object and set it in
     * SecurityContextHolder</li>
     * </ul>
     * </li>
     * <li>Continue the filter chain (always — even if JWT is invalid, to let
     * downstream filters return the proper 401/403 response)</li>
     * </ol>
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // ── Step 1: Extract the Authorization header ────────────────────────────
        final String authHeader = request.getHeader("Authorization");

        // If no Authorization header or not a Bearer token, skip JWT processing.
        // This allows public endpoints (e.g., /api/auth/login) to pass through
        // without triggering JWT validation.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // ── Step 2: Extract the JWT token (strip "Bearer " prefix) ──────────
        final String jwt = authHeader.substring(7); // "Bearer ".length() == 7
        final String username;

        try {
            username = jwtService.extractUsername(jwt);
        } catch (Exception e) {
            // Token is malformed, expired, or has an invalid signature.
            // Don't set authentication — let the chain return 401.
            log.debug("JWT extraction failed for request {}: {}", request.getRequestURI(), e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // ── Step 3: Validate and authenticate ───────────────────────────────────
        //
        // SecurityContextHolder.getContext().getAuthentication() == null means
        // no prior filter has authenticated this request. This check prevents
        // re-authentication if another filter already handled it.
        //
        if (username != null &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

            // Load user from the database (or wherever UserDetailsService is backed)
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Validate the token: check signature, expiry, and username match
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // ── Create the Authentication token ──────────────────────────────
                // UsernamePasswordAuthenticationToken is Spring Security's standard
                // representation of an authenticated user. It carries:
                // • principal: the UserDetails object
                // • credentials: null (we've validated via JWT, no password needed)
                // • authorities: the user's roles/permissions from UserDetails
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());

                // Attach request details (IP address, session ID) for audit logging
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                // ── Set the SecurityContext ───────────────────────────────────────
                // Once the SecurityContext has an Authentication object, all downstream
                // security checks (@PreAuthorize, URL authorization rules) will see
                // this request as authenticated with the user's roles.
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("JWT authenticated user '{}' for URI: {}",
                        username, request.getRequestURI());
            }
        }

        // ── Step 4: Continue the filter chain ───────────────────────────────────
        // ALWAYS call doFilter — even if authentication failed.
        // The authorization filter downstream will handle 401/403 responses.
        filterChain.doFilter(request, response);
    }
}
