package com.java.practice.ems.employee.dto;

import com.java.practice.ems.employee.entity.Employee;
import com.java.practice.ems.employee.entity.EmployeeStatus;
import com.java.practice.ems.employee.entity.EmployeeType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║ EmployeeResponse — Outbound DTO (Java 21 Record) ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * <h2>JAVA 21 RECORDS AS RESPONSE DTOs</h2>
 * <p>
 * Records are immutable, guaranteeing thread safety during serialization. They
 * map 1:1 via Jackson
 * without any additional getters/setters getters.
 * </p>
 */
public record EmployeeResponse(
        String id,
        String fullName,
        String email,
        String phone,
        EmployeeType type,
        String departmentName,
        String departmentId,
        BigDecimal baseSalary,
        LocalDate joinDate,
        EmployeeStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static EmployeeResponse from(Employee employee) {
        String deptName = employee.getDepartment() != null ? employee.getDepartment().getName() : null;
        String deptId = employee.getDepartment() != null ? employee.getDepartment().getId() : null;

        return new EmployeeResponse(
                employee.getId(),
                employee.getFullName(),
                employee.getEmail(),
                employee.getPhone(),
                employee.getType(),
                deptName,
                deptId,
                employee.getBaseSalary(),
                employee.getJoinDate(),
                employee.getStatus(),
                employee.getCreatedAt(),
                employee.getUpdatedAt());
    }
}
