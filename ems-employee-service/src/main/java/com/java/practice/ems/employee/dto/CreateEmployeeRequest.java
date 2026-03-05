package com.java.practice.ems.employee.dto;

import com.java.practice.ems.employee.entity.EmployeeType;
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
 * Java Records are purpose-built for "data carriers". They are the ideal DTO
 * implementation because:
 * 1. Immutability by design (fields are final) preventing mutation bugs.
 * 2. No boilerplate (constructors, getters, equals, hashCode are generated).
 * 3. Thread safety for concurrent request handling.
 * </p>
 */
public record CreateEmployeeRequest(

        @NotBlank(message = "Full name is required") @Size(min = 1, max = 100, message = "Full name must be between 1 and 100 characters") String fullName,

        @NotBlank(message = "Email is required") @Email(message = "Email must be a valid email address") @Size(max = 150, message = "Email must not exceed 150 characters") String email,

        @Size(max = 20, message = "Phone must not exceed 20 characters") String phone,

        @NotNull(message = "Employee type is required") EmployeeType type,

        @NotBlank(message = "Department ID is required") String departmentId,

        @DecimalMin(value = "0.0", inclusive = false, message = "Salary must be greater than zero") @Digits(integer = 13, fraction = 2, message = "Salary format invalid") BigDecimal baseSalary,

        LocalDate joinDate) {

    public CreateEmployeeRequest {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
        if (fullName != null) {
            fullName = fullName.trim();
        }
    }
}
