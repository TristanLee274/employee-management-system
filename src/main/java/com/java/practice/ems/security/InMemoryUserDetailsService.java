package com.java.practice.ems.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * In-memory implementation of {@link UserDetailsService} for development and
 * demo purposes.
 *
 * <h2>DEPENDENCY INVERSION PRINCIPLE</h2>
 *
 * <p>
 * Spring Security's authentication infrastructure depends on the
 * {@link UserDetailsService} <em>interface</em>, not on any specific
 * implementation.
 * This means:
 * </p>
 * <ul>
 * <li><strong>This class</strong>: In-memory hardcoded users (for
 * development)</li>
 * <li><strong>Production</strong>: Swap to a JPA-backed UserDetailsService that
 * loads users from the PostgreSQL database — zero changes to
 * SecurityConfig</li>
 * <li><strong>Enterprise</strong>: Swap to an LDAP or Keycloak-backed
 * implementation</li>
 * </ul>
 *
 * <p>
 * The {@code SecurityConfig} only knows about the interface. The container
 * decides which implementation to inject based on what {@code @Service} or
 * {@code @Bean} is available in the Spring context.
 * </p>
 *
 * <h2>PRINCIPLE OF LEAST PRIVILEGE — ROLE HIERARCHY</h2>
 *
 * <p>
 * Three demo users are defined with increasing privilege levels:
 * </p>
 * <ul>
 * <li><strong>viewer</strong> (ROLE_VIEWER): Can only READ employee data</li>
 * <li><strong>hr</strong> (ROLE_HR): Can read AND write employee data</li>
 * <li><strong>admin</strong> (ROLE_ADMIN): Full access including salary,
 * delete, config</li>
 * </ul>
 *
 * <p>
 * Each user receives ONLY the permissions needed for their job function —
 * the Principle of Least Privilege in action.
 * </p>
 */
@Service
@Slf4j
public class InMemoryUserDetailsService implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;

    public InMemoryUserDetailsService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Loads user credentials and authorities by username.
     *
     * <p>
     * <strong>Java 21 switch expression</strong> used here for type-safe,
     * null-safe user resolution. The compiler enforces exhaustive matching.
     * </p>
     *
     * <p>
     * In production, this would query a User table via JPA:
     * 
     * <pre>{@code
     * return userRepository.findByUsername(username)
     *         .map(user -> new User(user.getUsername(), user.getPassword(), user.getAuthorities()))
     *         .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
     * }</pre>
     *
     * @param username the login username
     * @return the UserDetails for the matched user
     * @throws UsernameNotFoundException if no user with that username exists
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user details for username='{}'", username);

        // ── Java 21 switch expression for clean user resolution ─────────────
        return switch (username) {
            // ROLE_VIEWER: Read-only access to employee listings
            case "viewer" -> User.builder()
                    .username("viewer")
                    .password(passwordEncoder.encode("viewer123"))
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_VIEWER")))
                    .build();

            // ROLE_HR: Read + Write access (create, update employees)
            case "hr" -> User.builder()
                    .username("hr")
                    .password(passwordEncoder.encode("hr123"))
                    .authorities(List.of(
                            new SimpleGrantedAuthority("ROLE_HR"),
                            new SimpleGrantedAuthority("ROLE_VIEWER")))
                    .build();

            // ROLE_ADMIN: Full access including delete, salary calculations
            case "admin" -> User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .authorities(List.of(
                            new SimpleGrantedAuthority("ROLE_ADMIN"),
                            new SimpleGrantedAuthority("ROLE_HR"),
                            new SimpleGrantedAuthority("ROLE_VIEWER")))
                    .build();

            // Unknown user → Spring Security returns 401 Unauthorized
            default -> throw new UsernameNotFoundException(
                    "User not found: '%s'".formatted(username));
        };
    }
}
