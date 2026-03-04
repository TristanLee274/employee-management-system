package com.java.practice.ems.employee.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Update request DTO for modifying an existing employee's data.
 *
 * <p>
 * <strong>Why a separate Update DTO instead of reusing
 * CreateEmployeeRequest?</strong>
 * </p>
 * <p>
 * Create and Update operations have different validation rules:
 * </p>
 * <ul>
 * <li><strong>Create:</strong> firstName, lastName, and email are REQUIRED (the
 * employee
 * doesn't exist yet, so we need all mandatory fields).</li>
 * <li><strong>Update:</strong> All fields are OPTIONAL — the client sends only
 * the fields
 * they want to change (partial update / PATCH semantics). Null fields are
 * ignored
 * by the service layer, keeping the original values.</li>
 * </ul>
 *
 * <p>
 * This separation follows the <strong>Interface Segregation Principle
 * (SOLID)</strong>:
 * API consumers see only the fields relevant to their operation. A client
 * updating a phone
 * number shouldn't need to re-send firstName and lastName.
 * </p>
 *
 * <p>
 * <strong>Java 21 Record for Update DTO:</strong> Even though all fields are
 * nullable
 * (optional), the Record is still immutable. The service layer reads these
 * fields to decide
 * what to update. The record itself is never modified after creation.
 * </p>
 *
 * @param firstName  Updated first name (null = keep current)
 * @param lastName   Updated last name (null = keep current)
 * @param email      Updated email (null = keep current, must be unique if
 *                   provided)
 * @param phone      Updated phone (null = keep current)
 * @param department Updated department (null = keep current)
 * @param jobTitle   Updated job title (null = keep current)
 * @param salary     Updated salary (null = keep current, must be positive if
 *                   provided)
 * @param hireDate   Updated hire date (null = keep current)
 * @param status     Updated employment status (null = keep current)
 */
public record UpdateEmployeeRequest(

        @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters") String firstName,

        @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters") String lastName,

        @Email(message = "Email must be a valid email address") @Size(max = 150, message = "Email must not exceed 150 characters") String email,

        @Size(max = 20, message = "Phone must not exceed 20 characters") String phone,

        @Size(max = 100, message = "Department must not exceed 100 characters") String department,

        @Size(max = 150, message = "Job title must not exceed 150 characters") String jobTitle,

        @DecimalMin(value = "0.0", inclusive = false, message = "Salary must be greater than zero") @Digits(integer = 10, fraction = 2, message = "Salary must have at most 10 integer digits and 2 decimal places") BigDecimal salary,

        LocalDate hireDate,

        // Status as a String to allow flexible parsing from the API.
        // The service layer converts this to EmployeeStatus enum with proper error
        // handling.
        String status) {
    /**
     * Compact constructor — normalizes input fields.
     * <p>
     * Unlike CreateEmployeeRequest, we do NOT enforce @NotBlank here because
     * all fields are optional for partial updates.
     */
    public UpdateEmployeeRequest {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
        if (firstName != null) {
            firstName = firstName.trim();
        }
        if (lastName != null) {
            lastName = lastName.trim();
        }
        if (status != null) {
            status = status.trim().toUpperCase();
        }
    }
}
