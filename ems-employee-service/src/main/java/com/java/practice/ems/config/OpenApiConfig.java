package com.java.practice.ems.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ OpenApiConfig — Swagger/OpenAPI 3.0 Interactive Documentation ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * <h2>SWAGGER/OPENAPI — THE IMPORTANCE OF AUTOMATED API DOCUMENTATION</h2>
 *
 * <p>
 * <strong>From a Team Lead perspective:</strong> API documentation is not a
 * "nice-to-have" — it is a <em>critical collaboration tool</em> that directly
 * impacts team velocity, onboarding time, and cross-team communication.
 * </p>
 *
 * <h3>Why Automated Documentation (not manual wikis)?</h3>
 *
 * <ol>
 * <li><strong>Single Source of Truth:</strong> The documentation is generated
 * directly from the code. When a developer adds a new endpoint and annotates
 * it with {@code @Operation}, the Swagger UI updates immediately. No wiki
 * page to update, no stale documentation to confuse new team members.
 *
 * <p>
 * In my experience as a Team Lead, manual API docs become outdated within
 * the first sprint. The endpoint changes, the wiki page doesn't. New developers
 * build against wrong specs, discover bugs at integration time, and we lose
 * 1-2 days per sprint in debugging miscommunication.
 * </p>
 * </li>
 *
 * <li><strong>Interactive Testing:</strong> Swagger UI provides a "Try It Out"
 * button for every endpoint. Frontend developers can test API responses
 * immediately without writing curl commands or Postman collections. This
 * removes the "Hey, is this endpoint working?" Slack messages that interrupt
 * the backend team.
 *
 * <p>
 * Real scenario: Frontend dev sees the contract, tests it live, builds
 * against it — all without a single Slack message to the backend team.
 * That's 3x faster frontend development.
 * </p>
 * </li>
 *
 * <li><strong>Contract-First Development:</strong> OpenAPI spec enables:
 * <ul>
 * <li>Code generation (SDK clients for mobile, frontend, partner teams)</li>
 * <li>Mock server generation for parallel development</li>
 * <li>API versioning and breaking change detection</li>
 * <li>Automated API testing in CI/CD pipelines</li>
 * </ul>
 * </li>
 *
 * <li><strong>Onboarding Acceleration:</strong> New team members can understand
 * ALL available endpoints within 30 minutes by browsing the Swagger UI. Without
 * it, they spend 2-3 days reading controllers, asking questions on Slack, and
 * reverse-engineering the API from frontend code.</li>
 *
 * <li><strong>Cross-Team Communication:</strong> In a microservices
 * architecture,
 * Team A consuming Team B's API can read the OpenAPI spec without scheduling
 * a meeting. The spec IS the contract — explicit, versioned, and testable.</li>
 * </ol>
 *
 * <h3>OpenAPI URLs (after application starts):</h3>
 * <ul>
 * <li>Swagger UI: {@code http://localhost:8080/swagger-ui.html}</li>
 * <li>OpenAPI JSON: {@code http://localhost:8080/v3/api-docs}</li>
 * <li>OpenAPI YAML: {@code http://localhost:8080/v3/api-docs.yaml}</li>
 * </ul>
 *
 * <h3>JWT Integration with Swagger</h3>
 * <p>
 * The configuration below adds a "Authorize" button to Swagger UI that lets
 * you paste a JWT token. All subsequent requests from the UI will include the
 * {@code Authorization: Bearer <token>} header — testing secured endpoints
 * becomes as easy as clicking a button.
 * </p>
 */
@Configuration
public class OpenApiConfig {

    /**
     * Configures the OpenAPI 3.0 specification for the EMS application.
     *
     * <p>
     * This bean customizes:
     * </p>
     * <ol>
     * <li>API metadata (title, version, description, contact)</li>
     * <li>JWT security scheme (adds "Authorize" button to Swagger UI)</li>
     * <li>Server URLs for different environments</li>
     * </ol>
     */
    @Bean
    public OpenAPI customOpenAPI() {
        // ── 1. Define the JWT security scheme ───────────────────────────────────
        // This tells Swagger UI how to authenticate API requests.
        // Type: HTTP + Scheme: bearer + Format: JWT
        // → Swagger UI will show an "Authorize" button where users paste their JWT
        final String securitySchemeName = "bearerAuth";
        SecurityScheme jwtScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("""
                        JWT Authentication. To get a token:
                        1. Call POST /api/auth/login with valid credentials
                        2. Copy the 'accessToken' from the response
                        3. Paste it here (without the 'Bearer ' prefix)

                        Demo users:
                        • viewer / viewer123 (read-only)
                        • hr / hr123 (read + write)
                        • admin / admin123 (full access)
                        """);

        return new OpenAPI()
                // ── 2. API Metadata ─────────────────────────────────────────────
                .info(new Info()
                        .title("Antigravity Employee Management System API")
                        .version("1.0.0")
                        .description("""
                                REST API for managing employees, departments, and payroll.

                                Built with Spring Boot 3.5.11, Java 21 Virtual Threads, and PostgreSQL.

                                **Authentication:** All endpoints (except /api/auth/**) require a JWT token.
                                Use the Authorize button above to set your token.

                                **Roles:**
                                - `ROLE_VIEWER` — Read-only access
                                - `ROLE_HR` — Read + Write access
                                - `ROLE_ADMIN` — Full access (including salary and delete operations)
                                """)
                        .contact(new Contact()
                                .name("EMS Development Team")
                                .email("dev-team@antigravity-ems.io"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))

                // ── 3. Security Configuration ───────────────────────────────────
                // Register the JWT scheme and apply it globally to all endpoints
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, jwtScheme))
                .addSecurityItem(new SecurityRequirement()
                        .addList(securitySchemeName))

                // ── 4. Server URLs ──────────────────────────────────────────────
                // Define environment-specific base URLs for the Swagger UI dropdown
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development"),
                        new Server().url("https://ems-staging.company.com").description("Staging"),
                        new Server().url("https://ems.company.com").description("Production")));
    }
}
