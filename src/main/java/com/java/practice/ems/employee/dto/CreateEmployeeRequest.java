package com.java.practice.ems.employee.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ CreateEmployeeRequest — Inbound DTO (Java 21 Record) ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * <h2>WHY JAVA 21 RECORDS ARE THE BEST CHOICE FOR DTOs</h2>
 *
 * <p>
 * Java Records (introduced in Java 16, mature in Java 21) are purpose-built for
 * "data carriers" — objects whose primary role is to hold and transport data
 * without
 * encapsulating behavior. They are the ideal DTO implementation because:
 * </p>
 *
 * <ol>
 * <li><strong>Immutability by design:</strong> Record fields are {@code final}.
 * Once a DTO is created, it cannot be modified. This eliminates an entire class
 * of bugs where a DTO is accidentally mutated between validation and
 * processing.
 * 
 * <pre>
 * // This is impossible with Records:
 * request.setEmail("hacker@evil.com"); // Compilation error!
 * </pre>
 * 
 * </li>
 *
 * <li><strong>No boilerplate:</strong> The compiler auto-generates:
 * <ul>
 * <li>{@code constructor} — canonical constructor with all fields</li>
 * <li>{@code getters} — named {@code firstName()} (not
 * {@code getFirstName()})</li>
 * <li>{@code equals()} — value-based equality (all fields compared)</li>
 * <li>{@code hashCode()} — consistent with {@code equals()}</li>
 * <li>{@code toString()} — includes all field names and values</li>
 * </ul>
 * This is ~60 lines of code saved per DTO compared to a traditional class.
 * </li>
 *
 * <li><strong>Value semantics:</strong> Two {@code CreateEmployeeRequest}
 * objects with
 * the same field values are {@code .equals()} — perfect for testing assertions:
 * 
 * <pre>
 * assertEquals(expectedRequest, actualRequest); // Works correctly!
 * </pre>
 * 
 * </li>
 *
 * <li><strong>Thread safety:</strong> Immutable objects are inherently
 * thread-safe.
 * With Virtual Threads handling thousands of concurrent requests, immutable
 * DTOs
 * prevent data races without synchronization overhead.</li>
 *
 * <li><strong>Serialization-friendly:</strong> Jackson serializes/deserializes
 * Records
 * seamlessly. Spring MVC's {@code @RequestBody} works with Records out of the
 * box
 * in Spring Boot 3.x.</li>
 *
 * <li><strong>Pattern matching (Java 21):</strong> Records support
 * deconstruction
 * in pattern matching, enabling expressive code:
 * 
 * <pre>
 *         if (request instanceof CreateEmployeeRequest(var fn, var ln, var email, ...)) {
 *             // Direct access to deconstructed components
 *         }
 * </pre>
 * 
 * </li>
 * </ol>
 *
 * <h2>WHY NOT USE THE ENTITY DIRECTLY AS THE REQUEST BODY?</h2>
 *
 * <p>
 * The entity ({@code Employee}) is a mutable, JPA-managed object with database
 * concerns (annotations like {@code @Id}, {@code @GeneratedValue}, audit
 * timestamps).
 * Using it as a request body would:
 * </p>
 * <ul>
 * <li>Allow clients to set the ID (overwriting existing records)</li>
 * <li>Allow clients to set {@code createdAt} / {@code updatedAt} (falsifying
 * audit data)</li>
 * <li>Couple the API schema to the database schema (changing one breaks the
 * other)</li>
 * <li>Expose internal fields in Swagger/OpenAPI documentation</li>
 * </ul>
 *
 * <h2>VALIDATION ANNOTATIONS</h2>
 *
 * <p>
 * Jakarta Bean Validation annotations ({@code @NotBlank}, {@code @Email}, etc.)
 * are evaluated by Spring MVC when the controller uses
 * {@code @Valid @RequestBody}.
 * Validation happens BEFORE the method body executes — invalid requests are
 * rejected
 * with a 400 Bad Request and structured error messages, never reaching the
 * service layer.
 * </p>
 *
 * @param firstName  Employee's first name (required, 1-100 chars)
 * @param lastName   Employee's last name (required, 1-100 chars)
 * @param email      Unique email address (required, must be valid format)
 * @param phone      Phone number (optional, max 20 chars)
 * @param department Department name (optional)
 * @param jobTitle   Job title/position (optional)
 * @param salary     Monthly salary in base currency (optional, must be
 *                   positive)
 * @param hireDate   Employment start date (optional, defaults to today in
 *                   service layer)
 */
public record CreateEmployeeRequest(

        @NotBlank(message = "First name is required") @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters") String firstName,

        @NotBlank(message = "Last name is required") @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters") String lastName,

        @NotBlank(message = "Email is required") @Email(message = "Email must be a valid email address") @Size(max = 150, message = "Email must not exceed 150 characters") String email,

        @Size(max = 20, message = "Phone must not exceed 20 characters") String phone,

        @Size(max = 100, message = "Department must not exceed 100 characters") String department,

        @Size(max = 150, message = "Job title must not exceed 150 characters") String jobTitle,

        @DecimalMin(value = "0.0", inclusive = false, message = "Salary must be greater than zero") @Digits(integer = 10, fraction = 2, message = "Salary must have at most 10 integer digits and 2 decimal places") BigDecimal salary,

        LocalDate hireDate) {
    // Records can have a "compact constructor" for additional validation
    // that goes beyond what annotations can express:

    /**
     * Compact constructor — validates cross-field business rules.
     * <p>
     * Annotation validators check individual fields. This constructor validates
     * relationships BETWEEN fields that annotations cannot express.
     */
    public CreateEmployeeRequest {
        // Normalize email to lowercase for consistent uniqueness checks.
        // This happens AFTER Jackson deserialization but BEFORE Bean Validation,
        // ensuring the @Email validator sees the normalized value.
        if (email != null) {
            email = email.trim().toLowerCase();
        }

        // Trim whitespace from name fields
        if (firstName != null) {
            firstName = firstName.trim();
        }
        if (lastName != null) {
            lastName = lastName.trim();
        }
    }
}
