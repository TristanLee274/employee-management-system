package com.java.practice.ems.employee.dto;

import com.java.practice.ems.employee.entity.EmployeeType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Update request DTO for modifying an existing employee's data. All fields are
 * optional.
 * 
 * <h2>WHY JAVA 21 RECORDS ARE THE BEST CHOICE FOR DTOs</h2>
 * <p>
 * They are immutable meaning what goes into validation stays exactly that
 * value, and generates less boilerplate code.
 * </p>
 */
public record UpdateEmployeeRequest(

        @Size(min = 1, max = 100, message = "Full name must be between 1 and 100 characters") String fullName,

        @Email(message = "Email must be a valid email address") @Size(max = 150, message = "Email must not exceed 150 characters") String email,

        @Size(max = 20, message = "Phone must not exceed 20 characters") String phone,

        EmployeeType type,

        String departmentId,

        @DecimalMin(value = "0.0", inclusive = false, message = "Salary must be greater than zero") @Digits(integer = 13, fraction = 2, message = "Salary format invalid") BigDecimal baseSalary,

        LocalDate joinDate,

        String status) {

    public UpdateEmployeeRequest {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
        if (fullName != null) {
            fullName = fullName.trim();
        }
        if (status != null) {
            status = status.trim().toUpperCase();
        }
    }
}
