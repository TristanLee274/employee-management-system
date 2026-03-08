package com.ems.employee.config;

import com.ems.employee.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ SecurityConfig — Spring Security 6.x Filter Chain Configuration ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * <h2>JWT (JSON WEB TOKEN) — WHY STATELESS AUTHENTICATION IS PREFERRED IN
 * MICROSERVICES</h2>
 *
 * <p>
 * Traditional session-based authentication stores session state on the server:
 * </p>
 * 
 * <pre>
 *   Client → POST /login → Server creates HttpSession (stored in memory/Redis)
 *   Client → GET /api/employees → Server looks up session → validates → responds
 * </pre>
 *
 * <p>
 * <strong>Problems with session-based auth in microservices:</strong>
 * </p>
 * <ol>
 * <li><strong>Horizontal Scaling Failure:</strong> If you have 5 pods behind a
 * load
 * balancer, the session exists on Pod #1. When the load balancer routes the
 * next
 * request to Pod #3, it has NO session → authentication fails. Solutions like
 * sticky sessions or centralized session stores (Redis) add complexity and a
 * single point of failure.</li>
 * <li><strong>Memory Overhead:</strong> Each active session consumes server
 * memory.
 * With 10,000 concurrent users, each session holding user data, roles,
 * permissions,
 * that's significant memory pressure per pod.</li>
 * <li><strong>Cross-Service Authentication:</strong> In a microservices
 * architecture,
 * Service A cannot validate a session created by Service B. You'd need a shared
 * session store — tight coupling that violates microservice independence.</li>
 * </ol>
 *
 * <p>
 * <strong>JWT stateless authentication solves all of these:</strong>
 * </p>
 * 
 * <pre>
 *   Client → POST /login → Server returns JWT (signed token, NOT stored on server)
 *   Client → GET /api/employees (Authorization: Bearer eyJhbGc...)
 *       → ANY pod validates JWT using the signing key → responds
 * </pre>
 *
 * <p>
 * Benefits:
 * </p>
 * <ul>
 * <li><strong>Stateless:</strong> No server-side session storage. ANY pod
 * validates
 * the JWT independently using the shared secret key or public key (RSA).</li>
 * <li><strong>Scalable:</strong> Add/remove pods freely. The JWT carries all
 * authentication data (user ID, roles, expiry) within the token itself.</li>
 * <li><strong>Cross-Service:</strong> All microservices share the same JWT
 * validation
 * key. Service A's token works in Service B without any shared database.</li>
 * <li><strong>Mobile-Friendly:</strong> Mobile apps store the JWT in secure
 * storage.
 * No cookie management, no CORS cookie issues.</li>
 * </ul>
 *
 * <h2>SPRING SECURITY FILTER CHAIN — THE SECURITY LAYER BEFORE THE
 * CONTROLLER</h2>
 *
 * <p>
 * Spring Security operates as a chain of servlet filters that intercept EVERY
 * incoming HTTP request <strong>before</strong> it reaches the Controller:
 * </p>
 *
 * <pre>
 *   HTTP Request
 *       │
 *       ▼
 *   ┌─────────────────────────────────────────────────────────┐
 *   │              Security Filter Chain (ordered)            │
 *   │  ┌──────────────────────────────────────────────┐       │
 *   │  │ 1. CORS Filter — validates cross-origin      │       │
 *   │  ├──────────────────────────────────────────────┤       │
 *   │  │ 2. CSRF Filter — disabled for stateless APIs │       │
 *   │  ├──────────────────────────────────────────────┤       │
 *   │  │ 3. JwtAuthenticationFilter (our custom)      │       │
 *   │  │    → Extracts JWT from Authorization header  │       │
 *   │  │    → Validates signature, expiry, claims     │       │
 *   │  │    → Sets SecurityContext if valid            │       │
 *   │  ├──────────────────────────────────────────────┤       │
 *   │  │ 4. AuthorizationFilter                       │       │
 *   │  │    → Checks URL patterns and roles           │       │
 *   │  │    → Returns 403 if insufficient permissions │       │
 *   │  ├──────────────────────────────────────────────┤       │
 *   │  │ 5. ExceptionTranslationFilter                │       │
 *   │  │    → Converts security exceptions to HTTP    │       │
 *   │  │      responses (401, 403)                    │       │
 *   │  └──────────────────────────────────────────────┘       │
 *   └─────────────────────────────────────────────────────────┘
 *       │
 *       ▼  (only if ALL filters pass)
 *   DispatcherServlet → Controller → Service → Repository
 * </pre>
 *
 * <p>
 * If ANY filter rejects the request, the controller is NEVER invoked.
 * This is a fundamental security benefit: business logic never sees
 * unauthenticated or unauthorized requests.
 * </p>
 *
 * <h2>PRINCIPLE OF LEAST PRIVILEGE — RBAC FOR SENSITIVE EMPLOYEE DATA</h2>
 *
 * <p>
 * <strong>Role-Based Access Control (RBAC)</strong> ensures that every user
 * gets the <em>minimum permissions</em> required for their job function:
 * </p>
 *
 * <pre>
 *   ┌───────────────────────────────────────────────────────────────────────┐
 *   │  Role          │  Permissions                                        │
 *   ├───────────────────────────────────────────────────────────────────────┤
 *   │  ROLE_VIEWER   │  GET /api/employees (list, read-only)               │
 *   │  ROLE_HR       │  GET + POST + PATCH /api/employees                  │
 *   │  ROLE_ADMIN    │  ALL endpoints including DELETE and salary calc      │
 *   │  PUBLIC        │  /api/auth/**, /swagger-ui/**, /actuator/health     │
 *   └───────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p>
 * <strong>Why Least Privilege matters for employee data:</strong>
 * </p>
 * <ul>
 * <li>Salary data is sensitive — only HR and Admin should see it</li>
 * <li>A compromised ROLE_VIEWER account can only READ, not modify or
 * delete</li>
 * <li>Deactivation/termination requires ROLE_ADMIN — prevents accidental data
 * loss</li>
 * <li>Audit trails (who accessed what) are meaningful because roles are
 * granular</li>
 * </ul>
 *
 * @see JwtAuthenticationFilter for the JWT extraction and validation filter
 */
@Configuration
@EnableWebSecurity
// @EnableWebSecurity:
// • Registers the Spring Security filter chain with the servlet container
// • Enables HTTP security configuration via SecurityFilterChain beans
// • Without this, Spring Boot's auto-config applies default security
// (all endpoints locked, generated password printed to console)

@EnableMethodSecurity
// @EnableMethodSecurity (Spring Security 6.x, replaces deprecated
// @EnableGlobalMethodSecurity):
// • Enables @PreAuthorize, @PostAuthorize, @Secured on individual methods
// • Example: @PreAuthorize("hasRole('ADMIN')") on a service method
// • This provides fine-grained, method-level access control IN ADDITION to
// the URL-pattern-based rules configured below
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    // ── Constructor Injection (Dependency Inversion Principle) ────────────────
    // We inject interfaces, NOT concrete implementations.
    // • JwtAuthenticationFilter is injected as a bean — we don't construct it here
    // • UserDetailsService is an interface — we can swap the implementation
    // (in-memory, database, LDAP) without changing this config class
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
            UserDetailsService userDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Defines the main security filter chain for the application.
     *
     * <p>
     * This method configures:
     * </p>
     * <ol>
     * <li>CSRF protection (disabled for stateless JWT APIs)</li>
     * <li>URL authorization rules (RBAC)</li>
     * <li>Session management (stateless)</li>
     * <li>Custom JWT filter registration</li>
     * <li>Authentication provider</li>
     * </ol>
     *
     * @param http the {@link HttpSecurity} builder provided by Spring
     * @return a fully configured {@link SecurityFilterChain}
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ── 1. CSRF PROTECTION ──────────────────────────────────────────────
                //
                // CSRF (Cross-Site Request Forgery) protection is DISABLED because:
                //
                // • Our API is stateless — no server-side session to forge
                // • JWT in the Authorization header is NOT automatically sent by the
                // browser (unlike cookies), so CSRF attacks are not applicable
                // • All state-changing requests require a valid JWT, which an attacker
                // cannot inject via a hidden form or image tag
                //
                // IMPORTANT: If you ever add cookie-based auth, RE-ENABLE CSRF!
                .csrf(AbstractHttpConfigurer::disable)

                // ── 2. CORS CONFIGURATION ───────────────────────────────────────────
                //
                // Enable CORS with default Spring Security settings.
                // For production, define a CorsConfigurationSource bean to whitelist
                // specific origins (e.g., https://ems.company.com).
                .cors(Customizer.withDefaults())

                // ── 3. URL-BASED AUTHORIZATION RULES (RBAC) ─────────────────────────
                //
                // PRINCIPLE OF LEAST PRIVILEGE applied through ordered rules:
                // • Rules are evaluated TOP-TO-BOTTOM — first match wins
                // • More specific rules MUST come before general rules
                // • Any request not matching a rule falls to the default:
                // .anyRequest().authenticated()
                //
                .authorizeHttpRequests(auth -> auth

                        // ── PUBLIC ENDPOINTS ────────────────────────────────────────────
                        // No authentication required for:
                        // • Authentication endpoints (login, register)
                        // • API documentation (Swagger UI, OpenAPI spec)
                        // • Health checks (Kubernetes liveness/readiness probes)
                        // • H2 console (development only — should be disabled in production)
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()

                        // ── ROLE-BASED ACCESS CONTROL ───────────────────────────────────
                        //
                        // READ operations: VIEWER, HR, and ADMIN can read employee data
                        .requestMatchers(HttpMethod.GET, "/api/employees/**")
                        .hasAnyRole("VIEWER", "HR", "ADMIN")

                        // WRITE operations: only HR and ADMIN can create/update employees
                        .requestMatchers(HttpMethod.POST, "/api/employees/**")
                        .hasAnyRole("HR", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/employees/**")
                        .hasAnyRole("HR", "ADMIN")

                        // DELETE/DEACTIVATION: only ADMIN can deactivate employees
                        // This is the most restrictive rule — Principle of Least Privilege
                        .requestMatchers(HttpMethod.DELETE, "/api/employees/**")
                        .hasRole("ADMIN")

                        // SALARY CALCULATIONS: sensitive financial data — ADMIN only
                        .requestMatchers("/api/employees/*/salary/**")
                        .hasRole("ADMIN")

                        // ── DEFAULT: DENY ALL UNMATCHED REQUESTS ────────────────────────
                        // Any request not matching a rule above requires authentication.
                        // This is the "deny by default" stance — secure by default.
                        .anyRequest().authenticated())

                // ── 4. SESSION MANAGEMENT (STATELESS) ───────────────────────────────
                //
                // SessionCreationPolicy.STATELESS tells Spring Security:
                // • NEVER create an HttpSession
                // • NEVER use an HttpSession to obtain the SecurityContext
                // • NEVER store authentication data in memory
                //
                // Every request must carry its own authentication (JWT in the
                // Authorization header). This is critical for:
                // • Horizontal scaling: any pod can handle any request
                // • Cloud deployment: pods are ephemeral, sessions would be lost
                // • Microservices: each service validates the JWT independently
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ── 5. AUTHENTICATION PROVIDER ──────────────────────────────────────
                //
                // The DaoAuthenticationProvider handles username/password validation:
                // • Loads user details from UserDetailsService (our custom implementation)
                // • Verifies the password against the BCrypt hash
                // • Returns an Authentication object with the user's roles/authorities
                .authenticationProvider(authenticationProvider())

                // ── 6. JWT FILTER REGISTRATION ───────────────────────────────────────
                //
                // Our custom JwtAuthenticationFilter is placed BEFORE Spring Security's
                // built-in UsernamePasswordAuthenticationFilter in the chain.
                //
                // WHY before? Because:
                // → For API requests, we want JWT authentication to happen first
                // → If the JWT is valid, we set the SecurityContext BEFORE the
                // authorization filter checks roles
                // → If no JWT is present, the request falls through to the next filter
                // (which will return 401 for protected endpoints)
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        // ── H2 Console Support (Development Only) ───────────────────────────────
        // H2 console uses frames — allow same-origin frames for the console to work.
        // In production, remove this line and disable the /h2-console endpoint.
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    /**
     * BCrypt password encoder bean.
     *
     * <p>
     * BCrypt is the industry standard for password hashing because:
     * </p>
     * <ul>
     * <li>Includes a built-in salt (no need for separate salt management)</li>
     * <li>Adaptive work factor (increase rounds as hardware gets faster)</li>
     * <li>Resistant to rainbow table attacks</li>
     * </ul>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * DAO-based authentication provider that loads users from a data source.
     *
     * <p>
     * <strong>Dependency Inversion in action:</strong> This provider depends
     * on the {@link UserDetailsService} interface. The actual implementation
     * (in-memory, database, LDAP) is injected at runtime. We can swap from
     * an in-memory user store to a PostgreSQL-backed store by simply providing
     * a different {@link UserDetailsService} bean — this config doesn't change.
     * </p>
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Exposes the {@link AuthenticationManager} as a bean for injection into
     * the authentication controller.
     *
     * <p>
     * This is needed to programmatically authenticate user credentials
     * during the login flow (POST /api/auth/login).
     * </p>
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
