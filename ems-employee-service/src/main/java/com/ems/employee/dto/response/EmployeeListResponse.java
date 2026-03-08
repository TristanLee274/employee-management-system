package com.ems.employee.dto.response;

import com.ems.employee.model.entity.Employee;
import com.ems.employee.model.entity.EmployeeStatus;

/**
 * Lightweight DTO for list views. Uses Java 21 Record for immutability.
 */
public record EmployeeListResponse(
        String id,
        String fullName,
        String email,
        String departmentName,
        EmployeeStatus status) {

    public static EmployeeListResponse from(Employee employee) {
        String deptName = employee.getDepartment() != null ? employee.getDepartment().getName() : null;
        return new EmployeeListResponse(
                employee.getId(),
                employee.getFullName(),
                employee.getEmail(),
                deptName,
                employee.getStatus());
    }
}
