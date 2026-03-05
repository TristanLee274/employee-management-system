package com.java.practice.ems.employee.dto;

import com.java.practice.ems.employee.entity.Employee;
import com.java.practice.ems.employee.entity.EmployeeStatus;

/**
 * Lightweight DTO for employee list views (e.g., employee directory, search
 * results).
 *
 * <p>
 * <strong>Why a separate list DTO?</strong>
 * </p>
 * <p>
 * Following the <strong>Interface Segregation Principle (SOLID)</strong>, API
 * consumers
 * requesting a list of employees don't need all 12 fields from
 * {@link EmployeeResponse}.
 * Sending full details for 500 employees wastes bandwidth and processing time.
 * </p>
 *
 * <p>
 * This lightweight record includes only the fields needed for a list/table
 * view:
 * </p>
 * <ul>
 * <li>Name and email for identification</li>
 * <li>Department and job title for organizational context</li>
 * <li>Status for filtering/display</li>
 * </ul>
 *
 * <p>
 * Sensitive data (salary) and verbose data (timestamps) are excluded.
 * The client fetches full details via {@code GET /employees/{id}} when needed.
 * </p>
 *
 * <p>
 * <strong>Java 21 Record advantage:</strong> Defining a second DTO is trivial —
 * just 6 lines of record header. With traditional classes, each DTO would need
 * ~80 lines of boilerplate, discouraging developers from creating
 * purpose-specific DTOs.
 * </p>
 *
 * @param id         Unique identifier
 * @param firstName  First name
 * @param lastName   Last name
 * @param email      Email address
 * @param department Department name
 * @param jobTitle   Job title
 * @param status     Employment status
 */
public record EmployeeListResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String department,
        String jobTitle,
        EmployeeStatus status) {

    /**
     * Factory method: Entity → List DTO.
     * <p>
     * Maps only the subset of fields needed for list views.
     * Salary, hire date, and timestamps are intentionally excluded.
     *
     * @param employee the JPA entity
     * @return a lightweight DTO for list display
     */
    public static EmployeeListResponse from(Employee employee) {
        return new EmployeeListResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getDepartment(),
                employee.getJobTitle(),
                employee.getStatus());
    }
}
